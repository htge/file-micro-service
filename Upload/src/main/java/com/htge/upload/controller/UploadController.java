package com.htge.upload.controller;

import com.htge.upload.config.UploadProperties;
import com.htge.upload.controller.util.FileHash;
import com.htge.upload.controller.util.FileItem;
import com.htge.upload.controller.util.FileOperationQueue;
import com.htge.upload.controller.util.FileOperationQueueManager;
import com.htge.upload.rabbit.login.LoginClient;
import com.htge.upload.rabbit.login.LoginData;
import com.htge.upload.rabbit.upload.UploadClient;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
            LoginData loginData;
            try {
                String sessionId = request.getRequestedSessionId();
                loginData = loginClient.getLoginInfo(sessionId);
                if (loginData == null) {
                    return new ModelAndView("error", "message", "RPC服务器出错，请联系管理员");
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
                    return new ModelAndView("error", "message", "所在的用户没有权限查看此页面");
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
     环境：i3_4170+机械硬盘+dropZone+2M_cache+4.38G文件
     Override模式
     无MD5用时：111s(323Mbps)
     加MD5用时：164s(218Mbps)
     创建模式
     加MD5用时：185s(193Mbps)
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Object uploadFile(MultipartHttpServletRequest request, HttpServletResponse response) {
        return getLoginStatus(request, response, () -> {
            JSONObject object = new JSONObject();
            File destFile = null;
            try {
                final MultipartFile file = request.getFile("file");
                if (file == null || file.isEmpty()) {
                    object.put("error", "file could not be empty!");
                    object.put("errorCode", 1);
                    return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
                }
                final String filename = file.getOriginalFilename();
                final String relativePath = request.getParameter("relative");
                final long fileSize = file.getSize();
                String totalStr = request.getParameter("total");
                Long total, offset, index;
                if (totalStr != null) {
                    total = Long.parseLong(request.getParameter("total"));
                    offset = Long.parseLong(request.getParameter("offset"));
                    index = Long.parseLong(request.getParameter("index"));
                } else {
                    //不是分块传输的情况
                    total = 1L;
                    offset = 0L;
                    index = 0L;
                }
                final String allHash = request.getParameter("all");
                final String hash = request.getParameter("hash");

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
                String part = FileHash.getMD5(inputStream, null);
                if (!part.equals(hash)) {
                    inputStream.close();
                    object.put("error", "Validation failed, network error?");
                    object.put("errorCode", 5);
                    logger.info("Validation failed, network error? index="+index);
                    return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
                }

                //回到流的初始端
                inputStream.reset();

                final String destFilename = uploadPath + "/" + filename;
                destFile = new File(destFilename);
                FileOperationQueue operationQueue;
                if (index == 0) {
                    operationQueue = FileOperationQueueManager.createInstance(destFile, total);
                } else {
                    operationQueue = FileOperationQueueManager.getInstance(destFile);
                }
                if (operationQueue == null) {
                    inputStream.close();
                    object.put("error", "internal queue error.");
                    object.put("errorCode", 6);

                    //删除文件，下次可以重来
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }
                    return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                //之后的inputStream由queue对象内部负责关闭
                operationQueue.addItem(new FileItem(inputStream, fileSize, index, offset));

                if (index+1 == total) {
                    operationQueue.waitAll();
                    if (!operationQueue.getMD5().equals(allHash)) {
                        object.put("error", "network error or man in middle.");
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
