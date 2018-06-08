package com.htge.login.model;

import com.htge.login.model.mapper.UserinfoMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component("UserinfoDao")
public class UserinfoDao {
	private SqlSessionFactoryBean factoryBean = null;
	private volatile UserItemCacheImpl userItemCache = null;
	private volatile ActionThread actionThread = null;

	public void setFactoryBean(SqlSessionFactoryBean factoryBean) {
		this.factoryBean = factoryBean;
	}

	public SqlSessionFactory getFactory() {
		try {
			return factoryBean.getObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setUserItemCache(UserItemCacheImpl userItemCache) {
		this.userItemCache = userItemCache;
		if (userItemCache != null) {
			userItemCache.clear(); //先清理缓存，以免数据不同步
			if (actionThread == null) {
				ActionThread thread = new ActionThread();
				thread.start();
				actionThread = thread;
			}
		} else {
			if (actionThread != null) {
				actionThread.quit();
				actionThread = null;
			}
		}
	}

	public UserItemCacheImpl getUserItemCache() {
		return userItemCache;
	}

	public boolean create(Userinfo userinfo) {
		if (userItemCache != null) {
			userItemCache.updateUser(userinfo);
			if (actionThread != null) {
				actionThread.addCreateItem(userinfo);
			}
			return true;
		}
		//同步数据库
		SqlSession session = getFactory().openSession();
		int ret = 0;
		try {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.insert(userinfo);
			if (ret != 0) {
				session.commit();
			}
		} catch (Exception e) {
			session.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
		return (ret != 0);
	}

	public boolean update(Userinfo userinfo) {
		if (userItemCache != null) {
			userItemCache.updateUser(userinfo);
			if (actionThread != null) {
				actionThread.addUpdateItem(userinfo);
			}
			return true;
		}
		//同步数据库
		SqlSession session = getFactory().openSession();
		int ret = 0;
		try {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.updateByUsername(userinfo);
			if (ret != 0) {
				session.commit(true);
			}
		} catch (Exception e) {
			session.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
		return (ret != 0);
	}

	public boolean delete(String username) {
		if (userItemCache != null) {
			userItemCache.deleteUser(username);
			if (actionThread != null) {
				actionThread.addDeleteItem(username);
			}
			return true;
		}
		//同步数据库
		SqlSession session = getFactory().openSession();
		int ret = 0;
		try {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.deleteByUsername(username);
			if (ret != 0) {
				session.commit(true);
			}
		} catch (Exception e) {
			session.rollback();
			e.printStackTrace();
		} finally {
			session.close();
		}
		return (ret != 0);
	}

	public Userinfo findUser(String username) {
		//缓存优先
		if (userItemCache != null) {
			Userinfo ret = userItemCache.findUser(username);
			if (ret != null) {
				return ret;
			}
		}

		//查数据库
		Userinfo ret = null;
		try (SqlSession session = getFactory().openSession()) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.selectByUsername(username);

			//同步到缓存
			if (userItemCache != null && ret != null) {
				userItemCache.updateUser(ret);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public Collection<Userinfo> getAllUser() {
		//缓存优先
		if (userItemCache != null) {
			Collection<Userinfo> ret = userItemCache.getAllUsers();
			if (ret != null) {
				return ret;
			}
		}

		//分块读取数据库
		List<Userinfo> list = new ArrayList<>();
		try (SqlSession session = getFactory().openSession()) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			List<Userinfo> tempList;
			final int limit = 1000;
			int begin = 0;
			do {
				tempList = mapper.select(begin, limit);
				if (tempList.size() > 0) {
					list.addAll(tempList);
					begin += list.size();
				}
			} while (tempList.size() == limit);

			//同步到缓存
			userItemCache.setAllUsers(list);
			if (list.size() == 0) {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	//等待数据库执行结束
	public void waitAction() {
		if (actionThread != null) {
			actionThread.waitUntilDone();
		}
	}

	private enum ActionItemType {
		create, update, delete
	}

	private class ActionThread extends Thread {
		private class ActionItem {
			Userinfo userinfo;
			ActionItemType type;

			ActionItem(Userinfo userinfo, ActionItemType type) {
				this.userinfo = userinfo;
				this.type = type;
			}

			ActionItem(String username) {
				userinfo = new Userinfo();
				userinfo.setUsername(username);
				this.type = ActionItemType.delete;
			}
		}

		private final List<List<ActionItem>> actionItemCache = new LinkedList<>(); //复合结构，为速度优化
		private List<ActionItem> actionItemList = new ArrayList<>(); //超过最大记录数则装入复合结构
		private final Lock lock = new ReentrantLock();
		private final int maxCount = 150;//最大记录数
		static final int autoSaveTime = 1000;//自动检测毫秒
		private volatile boolean isExecute = true;
		private final Timer timer = getTimer();

		private void checkCache() {
			if (actionItemList.size() > maxCount) {
				actionItemCache.add(actionItemList);
				actionItemList = new ArrayList<>();
			}
		}

		private Timer getTimer() { //一定时间后强制更换容器
			Timer ret = new Timer();
			ret.schedule(new TimerTask() {
				@Override
				public void run() {
					lock.lock();
					if (actionItemList.size() > 0) {
						actionItemCache.add(actionItemList);
						actionItemList = new ArrayList<>();
					}
					lock.unlock();
				}
			}, autoSaveTime, autoSaveTime);
			return ret;
		}

		void addCreateItem(Userinfo userinfo) {
			lock.lock();
			actionItemList.add(new ActionItem(userinfo, ActionItemType.create));
			checkCache();
			lock.unlock();
		}

		void addUpdateItem(Userinfo userinfo) {
			lock.lock();
			actionItemList.add(new ActionItem(userinfo, ActionItemType.update));
			checkCache();
			lock.unlock();
		}

		void addDeleteItem(String deleteItem) {
			lock.lock();
			actionItemList.add(new ActionItem(deleteItem));
			checkCache();
			lock.unlock();
		}

		private List<ActionItem> getActionItemList() {
			List<ActionItem> ret = null;
			lock.lock();
			if (actionItemCache.size() > 0) {
				ret = actionItemCache.get(0);
			}
			lock.unlock();
			return ret;
		}

		@Override
		public void run() {
			while (isExecute) {
				List<ActionItem> items = getActionItemList();
				if (items == null || items.size() <= 0) {
					try {
						sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
					continue;
				}
				SqlSession session = getFactory().openSession();
				try {
					UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
					//创建、插入、删除只取前面的部分
					for (ActionItem item : items) {
						switch (item.type) {
							case create:
								mapper.insert(item.userinfo);
								break;
							case update:
								mapper.updateByUsername(item.userinfo);
								break;
							case delete:
								mapper.deleteByUsername(item.userinfo.getUsername());
								break;
						}
					}
					session.commit();
				} catch (Exception e) {
					session.rollback();
					e.printStackTrace();
				} finally {
					session.close();
					lock.lock();
					actionItemCache.remove(0);
					lock.unlock();
				}
			}
		}

		void waitUntilDone() {
			do {
				lock.lock();
				boolean isDone = (actionItemCache.size() == 0 && actionItemList.size() == 0);
				lock.unlock();
				if (isDone) {
					break;
				}
				try {
					sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} while (true);
		}

		void quit() {
			isExecute = false;
			timer.cancel();
		}
	}
}
