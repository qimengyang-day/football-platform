package com.football.controller;

import com.football.common.Result;
import com.football.entity.SysUser;
import com.football.mapper.SysUserMapper;
import com.football.utils.JwtUtil;
import com.football.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload.path:uploads/}")
    private String uploadPath;

    @Value("${file.access.url:/uploads/}")
    private String accessUrl;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            return Result.error("只支持 JPG 和 PNG 格式的图片");
        }

        // 检查文件大小（最大2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("文件大小不能超过 2MB");
        }

        try {
            // 创建上传目录
            String avatarPath = uploadPath + "avatars/";
            Path uploadDir = Paths.get(avatarPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 获取当前用户ID并更新头像
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    String token = auth.replace("Bearer ", "");
                    Long userId = JwtUtil.getUserIdFromToken(token);
                    String fileUrl = accessUrl + "avatars/" + newFilename;
                    
                    SysUser user = sysUserMapper.selectById(userId);
                    if (user != null) {
                        user.setAvatar(fileUrl);
                        sysUserMapper.updateById(user);
                        
                        // 清除用户信息缓存
                        redisUtil.delete("user:info:" + userId);
                    }
                    
                    return Result.success(fileUrl);
                } catch (Exception e) {
                    System.err.println("更新用户头像失败: " + e.getMessage());
                }
            }

            // 返回访问URL
            String fileUrl = accessUrl + "avatars/" + newFilename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif"))) {
            return Result.error("只支持 JPG、PNG 和 GIF 格式的图片");
        }

        // 检查文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过 5MB");
        }

        try {
            // 创建上传目录
            String imagePath = uploadPath + "images/";
            Path uploadDir = Paths.get(imagePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadDir.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 返回访问URL
            String fileUrl = accessUrl + "images/" + newFilename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}