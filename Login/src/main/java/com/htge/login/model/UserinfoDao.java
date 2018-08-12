package com.htge.login.model;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.mapper.UserinfoMapper;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.jboss.logging.Logger;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserinfoDao {
	private SqlSessionFactory sessionFactory = null;
	private UserItemCacheImpl userItemCache = null;
	private final ActionThread actionThread = new ActionThread();
	private final Logger logger = Logger.getLogger(UserinfoDao.class);

	private LoginProperties loginProperties = null;

	public void setSessionFactory(SqlSessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public SqlSessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setLoginProperties(LoginProperties loginProperties) {
		this.loginProperties = loginProperties;
	}

	public UserinfoDao() {
		actionThread.start();
		new ActionHookThread(actionThread);
	}

	public void setUserItemCache(UserItemCacheImpl userItemCache) {
		this.userItemCache = userItemCache;
	}

	public UserItemCacheImpl getUserItemCache() {
		return userItemCache;
	}

	public boolean create(Userinfo userinfo) {
		if (userItemCache != null) {
			userItemCache.updateUser(userinfo);
			actionThread.addCreateItem(userinfo);
			return true;
		}
		//同步数据库
		SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE);
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
			session.clearCache();
			session.close();
		}
		return (ret != 0);
	}

	@SuppressWarnings("unused")
	public boolean createAll(List<Userinfo> userinfo) {
		if (userItemCache != null) {
			userItemCache.addUsers(userinfo);
		}
		SqlSession session = getSessionFactory().openSession(ExecutorType.BATCH);
		int ret = 0;
		try {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.insertList(userinfo);
			if (ret != 0) {
				session.commit();
			}
		} catch (Exception e) {
			session.rollback();
			session.clearCache();
			e.printStackTrace();
		} finally {
			session.clearCache();
			session.close();
		}
		return (ret != 0);
	}

	public boolean update(Userinfo userinfo) {
		if (userItemCache != null) {
			userItemCache.updateUser(userinfo);
			actionThread.addUpdateItem(userinfo);
			return true;
		}
		//同步数据库
		SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE);
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
			session.clearCache();
			session.close();
		}
		return (ret != 0);
	}

	public boolean delete(String username) {
		if (userItemCache != null) {
			userItemCache.deleteUser(username);
			actionThread.addDeleteItem(username);
			return true;
		}
		//同步数据库
		SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE);
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
			session.clearCache();
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
		try (SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE)) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			ret = mapper.selectByUsername(username);

			//同步到缓存
			if (userItemCache != null && ret != null) {
				userItemCache.updateUser(ret);
			}
		} catch (BindingException e) {
			logger.warn("UserinfoMapper was not ready");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public void buildUserCache() {
		//分块读取数据库
		try (SqlSession session = getSessionFactory().openSession(ExecutorType.BATCH)) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			List<Map<String, Long>> sizeList = mapper.size();
			final long count = sizeList.get(0).get("count");
			final long halfCount = count/2;
			//限制一次性访问数据库的数量，限制重建缓存最大数量
			//缓存的数据量与数据量和机器配置有关，还有局域网网速是否够快
			final long limit = loginProperties.getCacheUnit(), maxCacheLimit = loginProperties.getCacheTotal();
			int loadSize = 0;
			long begin = 0;
			do {
				List<Userinfo> tempList;
                //理论上可以减少20%左右的扫描时间
                if (begin > halfCount) {
                    long prevBegin = count-begin-limit;
                    long prevLimit = limit;

                    //最后几条数据的情况
                    if (prevBegin < 0) {
                        prevLimit = count-begin;
                        prevBegin = 0;
                    }

//				    logger.info("selectDesc("+prevBegin+","+prevLimit+")");
                    tempList = mapper.selectFromEnd(prevBegin, prevLimit);
                } else {
//                    logger.info("select("+begin+","+limit+")");
                    tempList = mapper.select(begin, limit);
                }
				if (tempList.size() > 0) {
					//同步到缓存
					if (userItemCache != null) {
						userItemCache.addUsers(tempList);
					}
					loadSize = tempList.size();
					begin += loadSize;
				}
			} while (loadSize == limit && begin < maxCacheLimit);
			if (userItemCache != null) {
				userItemCache.waitForAddUsers();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* 适用于小批量查询 */
	public Collection<Userinfo> getUsers(int begin, int limit) {
		//缓存优先会影响排序
//		if (userItemCache != null) {
//			Collection<Userinfo> ret = userItemCache.getUsers(begin, limit);
//			if (ret != null && ret.size() > 1) {
//				return ret;
//			}
//		}
		try (SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE)) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			List<Userinfo> userinfos = mapper.select((long)begin, (long)limit);
			//立即同步到缓存
			if (userItemCache != null) {
				userItemCache.addUsersSync(userinfos);
			}
			return userinfos;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/* 适用于小批量查询 */
	public Collection<Userinfo> getUsersExceptUser(String username, int begin, int limit) {
		try (SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE)) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			List<Userinfo> userinfos = mapper.selectExceptUser(username, (long)begin, (long)limit);
			//立即同步到缓存
			if (userItemCache != null) {
				userItemCache.addUsersSync(userinfos);
			}
			return userinfos;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Long getSize() {
		try (SqlSession session = getSessionFactory().openSession(ExecutorType.REUSE)) {
			UserinfoMapper mapper = session.getMapper(UserinfoMapper.class);
			return mapper.size().get(0).get("count");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//等待数据库执行结束
	public void waitAction() {
		actionThread.waitUntilDone();
	}

	public void autoBuildUserCache() {
		if (loginProperties.isCacheEnabled()) {
			//数据库的信息缓存到redis。数据量大的时候会很耗时，虽然一次性耗时长，但对大量数据的读写会有很大帮助
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			buildUserCache();
			stopWatch.stop();
			logger.info("autoBuildUserCache() elapsed: "+stopWatch.getTotalTimeMillis()+"ms");
		}
	}

	private enum ActionItemType {
		create, update, delete
	}

	private class ActionThread extends Thread {
		private final BlockingQueue<Collection<ActionItem>> actionItemCache = new LinkedBlockingDeque<>(); //复合结构，为速度优化
		private Collection<ActionItem> actionItemList = new LinkedList<>();
		private final Lock actionItemLock = new ReentrantLock();
		private final int maxCount = 1000;//最大记录数
		static final int autoSaveTime = 1000;//自动检测毫秒
		private boolean isExecute = true;
		private final Timer timer = getTimer();
		private final Object sessionObject = new Object(); //用于等待同步的
		private boolean isUpdate = false;
		private final Lock updateLock = new ReentrantLock();

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

		private void checkCache() {
			if (actionItemList.size() > maxCount) {
				actionItemCache.offer(actionItemList);
				actionItemList = new LinkedList<>();

				//强制更新以后，让计时器暂停一会
				setUpdate(true);
			}
		}

		public void setUpdate(boolean update) {
			updateLock.lock();
			isUpdate = update;
			updateLock.unlock();
		}

		public boolean isUpdate() {
			updateLock.lock();
			boolean result = isUpdate;
			updateLock.unlock();
			return result;
		}

		private Timer getTimer() { //一定时间后强制更换容器
			Timer ret = new Timer();
			ret.schedule(new TimerTask() {
				@Override
				public void run() {
					if (!isUpdate()) {
						actionItemLock.lock();
						if (actionItemList.size() > 0) {
							actionItemCache.offer(actionItemList);
							actionItemList = new LinkedList<>();
						}
						actionItemLock.unlock();
					} else {
						//暂停计时器后要恢复计时器
						setUpdate(false);
					}
				}
			}, autoSaveTime, autoSaveTime);
			return ret;
		}

		void addCreateItem(Userinfo userinfo) {
			actionItemLock.lock();
			actionItemList.add(new ActionItem(userinfo, ActionItemType.create));
			checkCache();
			actionItemLock.unlock();
		}

		void addUpdateItem(Userinfo userinfo) {
			actionItemLock.lock();
			actionItemList.add(new ActionItem(userinfo, ActionItemType.update));
			checkCache();
			actionItemLock.unlock();
		}

		void addDeleteItem(String deleteItem) {
			actionItemLock.lock();
			actionItemList.add(new ActionItem(deleteItem));
			checkCache();
			actionItemLock.unlock();
		}

		boolean isActionListEmpty() {
			actionItemLock.lock();
			boolean result = actionItemList.isEmpty();
			actionItemLock.unlock();
			return result;
		}

		@Override
		public void run() {
			Collection<ActionItem> items;
			SqlSession session = null;
			while (isExecute) {
				try {
					if (actionItemCache.isEmpty() && isActionListEmpty()) {
						synchronized (sessionObject) {
							sessionObject.notifyAll();
						}
					}
					items = actionItemCache.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
				if (session == null) {
					session = getSessionFactory().openSession(ExecutorType.BATCH);
				}
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
					StopWatch watch = new StopWatch();
					watch.start();
					session.commit();
					watch.stop();
					logger.info("session.commit() count = "+items.size()+" elapsed = "+watch.getTotalTimeMillis()+"ms");
				} catch (Exception e) {
					session.rollback();
					e.printStackTrace();
				}
			}
			if (session != null) {
				session.close();
			}
			synchronized (sessionObject) {
				sessionObject.notifyAll();
			}
			logger.info("ActionThread was exited");
		}

		void waitUntilDone() {
			if (actionItemCache.isEmpty() && isActionListEmpty()) {
				return;
			}
			synchronized (sessionObject) {
				try {
					sessionObject.wait();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		void quit() {
			timer.cancel();
			isExecute = false;
			actionItemCache.add(new ConcurrentLinkedQueue<>());
			synchronized (sessionObject) {
				try {
					sessionObject.wait();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class ActionHookThread extends Thread {
		ActionThread thread;
		private ActionHookThread(ActionThread thread) {
			this.thread = thread;
			Runtime.getRuntime().addShutdownHook(this);
		}

		@Override
		public void run() {
			thread.waitUntilDone();
			thread.quit();
		}
	}
}
