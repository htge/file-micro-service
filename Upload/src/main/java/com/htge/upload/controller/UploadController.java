package com.htge.upload.controller;

import com.htge.upload.config.UploadProperties;
import com.htge.upload.controller.util.FileHash;
import com.htge.upload.controller.util.FileItem;
import com.htge.upload.controller.util.FileOperationQueue;
import com.htge.upload.rabbit.login.LoginClient;
import com.htge.upload.rabbit.login.LoginData;
import com.htge.upload.rabbit.upload.UploadClient;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@Controller
@RequestMapping("/up")
public class UploadController {

    final private Logger logger = Logger.getLogger(UploadController.class);

    @Resource(name = "uploadClient")
    private UploadClient uploadClient;

    @Resource(name = "loginClient")
    private LoginClient loginClient;

    @Resource(name = "uploadProperties")
    private UploadProperties uploadProperties;

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    private Map<String, Object> buildFileTree(File dir) {
        File[] dirs = dir.listFiles((File pathname) -> {
            return pathname.isDirectory();
        });
        Map<String, Object> tree = null;
        if (dirs != null) {
            tree = new TreeMap<>();
            for (File subDir : dirs) {
                Map<String, Object> subTree = buildFileTree(subDir);
                tree.put(subDir.getName(), subTree);
            }
        }
        return tree;
    }

    private Object getLoginStatus(HttpServletRequest request, HttpServletResponse response, GetLoginStatusCallback callback) {
        if (uploadProperties.isAuthorization()) {
            LoginData loginData = null;
            try {
                String sessionId = request.getRequestedSessionId();
                if (sessionId != null) {
//			    loginData = new LoginData("{\"role\":1,\"isValidSession\":true,\"rootPath\":\"http://192.168.51.20/auth/\",\"logoutPath\":\"http://192.168.51.20/auth/logout\",\"settingPath\":\"http://192.168.51.20/auth/setting\"}");
                    final StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    loginData = loginClient.getLoginInfo(sessionId);
                    stopWatch.stop();
                    long millis = stopWatch.getTotalTimeMillis();
                    if (millis > 10) {
                        logger.warn("Upload elapsed: "+millis);
                    }
                }
                if (loginData == null) {
                    return new ModelAndView("rpcerror");
                }
                if (loginData.getErrorMessage() != null) {
                    logger.error("errorMessage = " + loginData.getErrorMessage());
                    response.sendRedirect(loginData.getRootPath());
                    return null;
                }
                if (!loginData.isValidSession()) {
                    response.sendRedirect(loginData.getRootPath());
                    return null;
                }
                if (loginData.getRole() != LoginData.LoginRole.Admin) {
                    return new ResponseEntity<>("No rule to show this page", HttpStatus.BAD_REQUEST);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return callback.execute();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Object uploadPage(HttpServletRequest request, HttpServletResponse response) {
        return getLoginStatus(request, response, () -> {
            Map<String, Object> root = buildFileTree(new File(uploadProperties.getRootPath()));
            ModelAndView uploadView = new ModelAndView("upload");
            uploadView.addObject("tree", root);
            uploadView.addObject("blockSize", uploadProperties.getBlockSize());
            return uploadView;
        });
    }

    @RequestMapping(value = "/upload/check", method = RequestMethod.GET)
    public Object checkFileExists(HttpServletRequest request, HttpServletResponse response) {
        return getLoginStatus(request, response, () -> {
            String relativeFile = request.getParameter("file");
            File serverFile = new File(uploadProperties.getRootPath() + "/" + relativeFile);
            if (serverFile.exists()) {
                return new ResponseEntity<>("{\"result\": 1}", HttpStatus.OK);
            }
            return new ResponseEntity<>("{\"result\": 0}", HttpStatus.OK);
        });
    }

    /*
     单任务：
     环境：MBA 2015 1.6Ghz
     文件大小：5.63G
     无MD5用时：101s(446.Mbps)
     加MD5用时：250s，扣除最后全文件校验25.6s后为224.4s(200Mbps)
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Object uploadFile(MultipartHttpServletRequest request, HttpServletResponse response) {
        return getLoginStatus(request, response, () -> {
            JSONObject object = new JSONObject();
            File destFile = null;
            try {
                final MultipartFile file = request.getFile("data");
                if (file == null || file.isEmpty()) {
                    object.put("error", "file could not be empty!");
                    object.put("errorCode", 1);
                    return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
                }
                final String filename = request.getParameter("fileName");
                final String total = request.getParameter("total");
                final String relativePath = request.getParameter("relative");
                final String md5 = request.getParameter("md5");
                final String partMD5 = request.getParameter("partMD5");
                final String index = request.getParameter("index");
                final String blockSize = request.getParameter("blockSize");

                //准备文件信息
                final String uploadPath = uploadProperties.getRootPath() + "/" + relativePath;
                final File dir = new File(uploadPath);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        object.put("error", "dir could not be created!");
                        object.put("errorCode", 2);
                        return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

                //校验MD5
                final InputStream inputStream = file.getInputStream();
                if (!FileHash.getMD5(inputStream, null).equals(partMD5)) {
                    inputStream.close();
                    object.put("error", "Validation failed, network error?");
                    object.put("errorCode", 5);
                    return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                //回到流的初始端
                inputStream.reset();

                final String destFilename = uploadPath + "/" + filename;
                destFile = new File(destFilename);
                FileOperationQueue operationQueue = null;
                if (index.equals("1")) {
                    operationQueue = FileOperationQueue.createInstance(destFile, Long.parseLong(total), Long.parseLong(blockSize));
                } else {
                    operationQueue = FileOperationQueue.getInstance(destFile);
                    if (operationQueue == null) {
                        object.put("error", "internal queue error.");
                        object.put("errorCode", 6);

                        //删除文件，下次可以重来
                        if (!destFile.delete()) {
                            logger.warn("Delete file \""+destFilename+"\" failed?");
                        }
                        return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
                operationQueue.addItem(new FileItem(inputStream, file.getSize(), Long.parseLong(index)));

                if (index.equals(total)) {
                    operationQueue.waitAll();
                    if (!FileHash.getFileMD5(destFile).equals(md5)) {
                        object.put("error", "Validation failed, network error?");
                        object.put("errorCode", 7);

                        //删除文件，下次可以重来
                        if (!destFile.delete()) {
                            logger.warn("Delete file \""+destFilename+"\" failed?");
                        }
                        return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    //给下载服务器发验证好的文件信息
                    if (!uploadClient.setUploadFileETag(destFilename, FileHash.getFileETag(destFile))) {
                        object.put("error", "RPC failed!");
                        object.put("errorCode", 8);

                        //删除文件，下次可以重来
                        if (!destFile.delete()) {
                            logger.warn("Delete file \""+destFilename+"\" failed?");
                        }
                        return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                object.put("error", e.getMessage());
                object.put("errorCode", -1);
                //删除文件，下次可以重来
                if (destFile != null) {
                    if (!destFile.delete()) {
                        logger.warn("Delete file \"" + destFile + "\" failed?");
                    }
                }
                return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
            }
            //{}
            return new ResponseEntity<>(object, HttpStatus.OK);
        });
    }

    private interface GetLoginStatusCallback {
        Object execute();
    }
}
