package com.example.aicodeproductbackend.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {
    public static final String FILE_SACE_ROOT_DIR=System.getProperty("user.dir")+"/tmp/code_output/";

    /**
     * 生成单个html文件
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult){
        String buildUniqueDir = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        writeToFile(buildUniqueDir,"index.html",htmlCodeResult.getHtmlCode());
        return new File(buildUniqueDir);

    }
    /**
     * 生成多个文件
     */
    public static File saveMultipleFileCodeResult(MultiFileCodeResult multiFileCodeResult){
        String buildUniqueDir = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        writeToFile(buildUniqueDir,"index.html",multiFileCodeResult.getHtmlCode());
        writeToFile(buildUniqueDir,"index.css",multiFileCodeResult.getCssCode());
        writeToFile(buildUniqueDir,"index.js",multiFileCodeResult.getJsCode());
        return new File(buildUniqueDir);
    }
    /**
     * 写单个文件
     * @param buildUniqueDir
     * @param filename
     * @param htmlCode
     */
    private static void writeToFile(String buildUniqueDir, String filename, String htmlCode) {
        String filePath = buildUniqueDir + File.separator + filename;
        FileUtil.writeString(htmlCode,filePath, StandardCharsets.UTF_8);
    }

    /**
     * 生成唯一目录
     * @param  value
     * @return filePath
     */
    private static String buildUniqueDir(String value) {
        String format = StrUtil.format("{}{}", value, IdUtil.getSnowflakeNextIdStr());
        String filePath = FILE_SACE_ROOT_DIR + File.separator + format;
        FileUtil.mkdir(filePath);
        return filePath;

    }
}
