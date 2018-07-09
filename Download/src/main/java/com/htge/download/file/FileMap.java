package com.htge.download.file;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.htge.download.config.FileProperties;
import com.htge.download.file.cache.FileETagCache;
import com.htge.download.file.util.FileHash;
import com.htge.download.rabbit.login.LoginClient;
import com.htge.download.rabbit.login.LoginData;
import org.jboss.logging.Logger;
import org.springframework.http.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

public class FileMap {
	private LoginClient loginClient = null;
	private FileProperties properties;
	private FileETagCache eTagCache;

	private final Logger logger = Logger.getLogger(FileMap.class);

	public void setLoginClient(LoginClient loginClient) {
		this.loginClient = loginClient;
	}

	public void setProperties(FileProperties properties) {
		this.properties = properties;
	}

	public void seteTagCache(FileETagCache eTagCache) {
		this.eTagCache = eTagCache;
	}

	public Object list(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String contextPath = URLDecoder.decode(request.getRequestURI(), "UTF-8");
		String relativePath = "";

		//请求路径特别短，末尾不含"/"，跳转
		if (contextPath.length() < properties.getServerRoot().length() &&
				contextPath.lastIndexOf("/") != contextPath.length()-1) {
			return new ModelAndView("redirect:"+contextPath+"/");
		}
		//处理相对路径
		int index = contextPath.indexOf(properties.getServerRoot());
		int beginIndex = index + properties.getServerRoot().length() - 1;
		if (contextPath.length() >= beginIndex) {
			relativePath = contextPath.substring(beginIndex);
			if (relativePath.length() > 1 && relativePath.indexOf("/") != 0) {
				relativePath = "/" + relativePath;
			}
		}

		boolean isPath = contextPath.lastIndexOf('/') == contextPath.length()-1;
		String rangeStr = request.getHeader("range");
		String basePath = properties.getLocalDir()+relativePath;
		FileRange fileRange = null;
		boolean isRangeTransport = false;
		File file = new File(basePath);
        LoginData loginData = null; //登录信息
		String eTag = null; //下载文件专用值
		if (!isPath) { //文件的情况下，做一些判断
			if (file.exists()) {
				if (file.isDirectory()) { //检测到是文件夹的时候，自动跳转
					return redirectToDirectory(contextPath);
				}

				//是文件，提前算好eTag，下载的时候重用值，不用重复计算了
				eTag = eTagCache.getETag(file.toPath());
				//拿不到缓存的情况下
				if (eTag == null) {
					logger.info("Generate ETag from file: " + file.toString());
					eTag = FileHash.getFileETag(file);
				}

				//只有断点续传的时候，要求ETag与if-range头匹配（会导致下载工具无法起作用）
//				String ifRangeStr = request.getHeader("if-range");
//				if (rangeStr != null && !eTag.equals(ifRangeStr)) {
//					HttpHeaders headers = new HttpHeaders();
//					headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//					headers.set("Accept-Ranges", "bytes");
//					return new ResponseEntity<>(headers, HttpStatus.BAD_REQUEST);
//				}

				//范围检查，遇到不正确的范围则返回错误
				fileRange = generateFileRange(rangeStr, file);
				if (fileRange == null) {
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
					headers.set("Accept-Ranges", "bytes");
					return new ResponseEntity<byte[]>(headers, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
				}
				isRangeTransport = ((request.getRequestedSessionId() != null) && (fileRange.getStart() > 1024)); //断点续传请求的条件
			}
		}
		if (!isRangeTransport && properties.isAuthorization()) {
			//去授权服务器获取信息：
			//1. 是否登录，授权服务器检测到未登录，那么返回跳转数据，由这里组
			//2. 权限，是否管理员
			//getRequestedSessionId()要能拿到数据，需要在application.properties配置server.session.cookie.name
			String sessionId = request.getRequestedSessionId();
			if (sessionId != null) {
//			    loginData = new LoginData("{\"role\":1,\"isValidSession\":true,\"rootPath\":\"http://192.168.51.20/auth/\",\"logoutPath\":\"http://192.168.51.20/auth/logout\",\"settingPath\":\"http://192.168.51.20/auth/setting\"}");
				loginData = loginClient.getLoginInfo(sessionId);
			}
			if (loginData == null) {
				return new ModelAndView("rpcerror");
			}
			if (loginData.getErrorMessage() != null) {
                logger.error("errorMessage = "+loginData.getErrorMessage());
                response.sendRedirect(loginData.getRootPath());
                return null;
            }
            if (!loginData.isValidSession()) {
                response.sendRedirect(loginData.getRootPath());
                return null;
            }
		}
		//以下条件，必须要先检查完登录再确定
		if (isPath) { //路径
			if (relativePath.length() == 0) { //跳转
				return new ModelAndView("redirect:"+properties.getServerRoot());
			}
			if (file.isDirectory()) {
				return list(contextPath, basePath, loginData); //显示列表信息
			} else { //文件夹不存在
				return new ModelAndView("notfound");
			}
		} else {
			if (!file.exists()) { //文件不存在
				return new ModelAndView("notfound");
			}
		}
		if (fileRange != null) {
			return downloadWithRange(fileRange, file, eTag, request);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.set("Accept-Ranges", "bytes");
		return new ResponseEntity<byte[]>(headers, HttpStatus.BAD_REQUEST);
	}

	private class FileInfo {
	    boolean isFile;
        long modified;
        String size;
        String name;
        String encodedName;
        String lastModified;

        private FileInfo(File file) {
            try {
                isFile = file.isFile();
                modified = file.lastModified();
                lastModified = getLastModifiedStr(modified);
                String filename = file.getName();
                if (isFile) {
                    name = filename;
                    encodedName =  URLEncoder.encode(filename, "UTF-8");
                    size = getFileSizeStr(file.length());
                } else {
                    name = filename + "/";
					encodedName =  URLEncoder.encode(filename, "UTF-8") + "/";
                    size = "";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        private long getModified() {
            return modified;
        }

        private Map<String, Object> mapValue() {
            Map<String, Object> map = new HashMap<>();
            map.put("isfile", isFile);
            map.put("name", name);
            map.put("encodedName", encodedName);
            map.put("size", size);
            map.put("lastmodified", lastModified);
            return map;
        }

        private String getFileSizeStr(double size) {
            int nUnit = 0;
            String unit[] = {"字节", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
            while (size > 1024) {
                size /= 1024;
                nUnit++;
            }
            if (nUnit > unit.length) {
                return "Out of range";
            }
            return String.format("%.2f %s", size, unit[nUnit]);
        }

        private String getLastModifiedStr(long lastModified) {
            Date dt = new Date(lastModified);
            SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sm.format(dt);
        }
    }

	private ModelAndView list(String contextPath, String basePath, LoginData loginData) {
		File file = new File(basePath);
		if (!file.exists()) {
			return new ModelAndView("notfound");
		}
		String basePathName = "", parentPathName = ""; //具体用途见底下的数据结构
		if (!contextPath.equalsIgnoreCase(properties.getServerRoot())) { //首页不显示返回上一级
			String []components = contextPath.split("/");
			if (components.length >= 2) {
				basePathName = components[components.length-1];
				parentPathName = components[components.length-2];
			}
		}
		FilenameFilter filter = (File dir, String name) -> { //排除.DS_Store这类文件
		    return name.indexOf(".") != 0;
        };
		File[] files = file.listFiles(filter);
		List<Map<String, Object>> dataSources = new ArrayList<>();
		if (files != null) {
		    List<FileInfo> modified = new ArrayList<>();
            logger.info("basePath:" + basePath + "\tlist.size:" + files.length);
		    for (File f : files) {
                modified.add(new FileInfo(f));
            }
            modified.sort((FileInfo o1, FileInfo o2) -> {
                long ret = o1.getModified()-o2.getModified();
                if (ret < 0) {
                    return 1;
                }
                if (ret > 0) {
                    return -1;
                }
                return 0;
            });
			for (FileInfo f : modified) {
//				logger.info("file name:" + f.getFilename());

                try {
                    dataSources.add(f.mapValue());
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}
		}

		ModelAndView result = new ModelAndView("filelist");
		if (loginData != null) {
			if (loginData.getRole() == LoginData.LoginRole.Admin) {
				String settingPath = loginData.getSettingPath();
				result.addObject("role", "1");

				if (settingPath != null) {
					result.addObject("settingPath", settingPath);
				}
			}
			result.addObject("logoutPath", loginData.getLogoutPath());
		}
        if (basePathName.length() > 0) {
            result.addObject("title", basePathName);
        }
		result.addObject("back", parentPathName);
		result.addObject("contextpath", contextPath);
		result.addObject("datasrcs", dataSources);
		return result;
	}

	private ResponseEntity downloadWithRange(FileRange fileRange, File file, String eTag, HttpServletRequest request) {
		long start = fileRange.getStart();
		long size = fileRange.getEnd() - start + 1;
		if (size > 0) {
			HttpHeaders headers = new HttpHeaders();
			HttpStatus status = HttpStatus.OK;
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			headers.setContentLength(size);
			headers.set("Accept-Ranges", "bytes");
			if (fileRange.isRangeMode()) {
				status = HttpStatus.PARTIAL_CONTENT;
				headers.set("Content-Range", String.format("bytes %d-%d/%d",
						fileRange.getStart(), fileRange.getStart() + size - 1, fileRange.getTotal()));
			}
			headers.setLastModified(file.lastModified());
			headers.setETag(eTag);

			//具体操作放在Filter里面处理
			request.setAttribute("file", file);
			request.setAttribute("fileRange", fileRange);
			return new ResponseEntity<byte[]>(headers, status);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.set("Accept-Ranges", "bytes");
		return new ResponseEntity<byte[]>(headers, HttpStatus.BAD_REQUEST);
	}

	private FileRange generateFileRange(String range, File file) {
		if (range == null) { //正常下载
			return new FileRange(file.length());
		} else { //断点续传
			String[] ranges = range.split("=");
			if (ranges.length == 2) { //Range: bytes=xxx-xxx
				range = ranges[1].replace(" ", ""); //xxx-xxx
				try {
					return new FileRange(range, file.length());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private ModelAndView redirectToDirectory(String contextPath) throws UnsupportedEncodingException {
		String []splits = contextPath.split("/");
		StringBuilder url = new StringBuilder("/");
		for (String split : splits) { //中文问题
			if (split.length() == 0) {
				continue;
			}
			split = URLEncoder.encode(split, "UTF-8");
			url.append(split);
			url.append("/");
		}
		RedirectView view = new RedirectView(url.toString());
		return new ModelAndView(view);
	}
}
