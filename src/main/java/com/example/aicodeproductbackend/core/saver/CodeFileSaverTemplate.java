package com.example.aicodeproductbackend.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract  class CodeFileSaverTemplate<T> {
    //定义文件保存根目录
     protected static final String FILE_SAVE_ROOT_DIR= System.getProperty("user.dir") + "/temp/code_output";
     public final File save(T result,Long appId){
         //1.验证输入
         validateInput(result);
         //2.创建文件目录
         String baseDir=bulidUniqueDir(appId);
         //保存文件
         saveFiles(result,baseDir);
         //返回文件对象
         return new File(baseDir);

     }

    protected abstract void saveFiles(T result, String baseDir);

    protected String bulidUniqueDir(Long appId) {
         if (appId==null){
             throw new BusinessException(ErrorCode.SYSTEM_ERROR,"appId不能为空");
         }
        String codeType = getCodeGenType().getValue();
        String format = StrUtil.format("{}_{}", codeType, appId);
        String filePath = FILE_SAVE_ROOT_DIR + File.separator + format;
        FileUtil.mkdir(filePath);
        return filePath;


    }

    protected void validateInput(T result) {
    if (result==null){
        throw new BusinessException(ErrorCode.SYSTEM_ERROR,"请求参数不能为空");
    }
    }
    protected abstract CodeGenTypeEnum getCodeGenType();

    protected final void writeToFile(String filename, String content, String baseDir) {
        if (StrUtil.isNotBlank(content)){
            FileUtil.writeString(content, baseDir + File.separator + filename, StandardCharsets.UTF_8);
        }

    }
}
