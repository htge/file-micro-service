package com.htge.upload.controller;

import com.htge.upload.config.UploadProperties;
import com.htge.upload.controller.util.*;
import com.htge.upload.rabbit.login.LoginData;
import com.htge.upload.rabbit.upload.UploadClient;
import net.sf.json.JSONObject;
import org.jboss.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@RequestMapping("/up")
public class UploadController {
    final private Logger logger = Logger.getLogger(UploadController.class);

    private UploadUtil uploadUtil;
    private UploadClient uploadClient;
    private UploadProperties uploadProperties;

    public void setUploadUtil(UploadUtil uploadUtil) {
        this.uploadUtil = uploadUtil;
    }
    public void setUploadClient(UploadClient uploadClient) {
        this.uploadClient = uploadClient;
    }
    public void setUploadProperties(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @GetMapping("")
    public Object uploadPageRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getServletPath();
        response.sendRedirect(path+"/");
        return null;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public Object uploadPage(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return uploadUtil.getLoginStatus(request, (UploadUtil.LoginStatus status, LoginData loginData) -> {
            switch (status) {
                case RPC_Failed:
                    throw new Exception("RPC服务器出错，请联系管理员");
                case NOT_Logined:
                    if (loginData.getErrorMessage() != null) {
                        logger.error("errorMessage = " + loginData.getErrorMessage());
                    }
                    response.sendRedirect(loginData.getRootPath());
                    return null;
                case LOGIN_Denied:
                    ModelAndView view = new ModelAndView("error", HttpStatus.BAD_REQUEST);
                    view.addObject("errorMessage", "所在的用户没有权限查看此页面");
                    return view;
                case Success:
                    break;
            }
            ArrayList<JSONObject> arrayList = new ArrayList<>();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", "/");
            jsonObject.put("pId", 0);
            jsonObject.put("name", "/");
            jsonObject.put("open", true);
            arrayList.add(jsonObject);
            uploadUtil.buildFileList(new File(uploadProperties.getRootPath()), "/", arrayList);
            String arrayStr = arrayList.toString();
            ModelAndView uploadView = new ModelAndView("upload");
            uploadView.addObject("tree", arrayStr);
            return uploadView;
        });
    }

    /*
     主要的压力瓶颈在前端的JS和服务器硬盘
     */
    @ResponseBody
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Object uploadFile(MultipartHttpServletRequest request) throws Exception {
        class UploadErrorCode {
            private static final int ERROR_FILE_EMPTY = 1;
            private static final int ERROR_DIR_NOT_EXISTS = 2;
            private static final int ERROR_NETWORK = 3;
            private static final int ERROR_RPC = 4;
            private static final int ERROR_NOT_LOGINED = 5;
            private static final int ERROR_LOGIN_DENIED = 6;
            private static final int ERROR_INTERNAL_QUEUE = 7;
        }

        JSONObject object = new JSONObject();

        final MultipartFile file = request.getFile("file");
        if (file == null || file.isEmpty()) {
            object.put("error", "文件没有内容");
            object.put("errorCode", UploadErrorCode.ERROR_FILE_EMPTY);
            return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
        }

        //准备文件信息
        final String relativePath = request.getParameter("relative");
        final String uploadPath = uploadProperties.getRootPath() + relativePath;
        final File dir = new File(uploadPath);
        if (!dir.exists()) {
            object.put("error", "指定的目录不存在");
            object.put("errorCode", UploadErrorCode.ERROR_DIR_NOT_EXISTS);
            return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        final String filename = file.getOriginalFilename();
        final String destFilename = uploadPath + "/" + filename;
        final File destFile = new File(destFilename);

        return uploadUtil.getLoginStatus(request, (UploadUtil.LoginStatus status, LoginData loginData) -> {
            switch (status) {
                case RPC_Failed:
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }

                    object.put("error", "服务器内部错误：登录RPC异常");
                    object.put("errorCode", UploadErrorCode.ERROR_RPC);
                    return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                case NOT_Logined:
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }

                    if (loginData.getErrorMessage() != null) {
                        object.put("error", loginData.getErrorMessage());
                    } else {
                        object.put("error", "未登录");
                    }
                    object.put("errorCode", UploadErrorCode.ERROR_NOT_LOGINED);
                    return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
                case LOGIN_Denied:
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }

                    object.put("error", "没有权限");
                    object.put("errorCode", UploadErrorCode.ERROR_LOGIN_DENIED);
                    return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
                case Success:
                    break;
            }

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

            //校验MD5
            final InputStream inputStream = file.getInputStream();
            String part = FileHash.getMD5(inputStream, null);
            if (!part.equals(hash)) {
                inputStream.close();
                object.put("error", "网络异常，校验失败");
                object.put("errorCode", UploadErrorCode.ERROR_NETWORK);
                logger.info("Validation failed, network error? index="+index);
                return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
            }

            //回到流的初始端
            inputStream.reset();

            FileOperationQueue operationQueue;
            if (index == 0) {
                operationQueue = FileOperationQueueManager.createInstance(destFile, total);
            } else {
                operationQueue = FileOperationQueueManager.getInstance(destFile);
            }
            if (operationQueue == null) {
                inputStream.close();
                object.put("error", "内部队列异常");
                object.put("errorCode", UploadErrorCode.ERROR_INTERNAL_QUEUE);

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
                    object.put("error", "网络错误，可能存在中间人攻击");
                    object.put("errorCode", UploadErrorCode.ERROR_NETWORK);

                    //删除文件，下次可以重来
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }
                    return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                //给下载服务器发验证好的文件信息
                if (!uploadClient.setUploadFileETag(destFilename, FileHash.getFileETag(destFile))) {
                    object.put("error", "服务器内部错误：下载RPC异常");
                    object.put("errorCode", UploadErrorCode.ERROR_RPC);

                    //删除文件，下次可以重来
                    if (!destFile.delete()) {
                        logger.warn("Delete file \""+destFilename+"\" failed?");
                    }
                    return new ResponseEntity<>(object, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            return object;
        });
    }
}
