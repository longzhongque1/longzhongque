package com.example.aicodeproductbackend.core.saver;

import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

public class MultiFileCodeSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult>{
    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDir) {
        writeToFile("index.html", result.getHtmlCode(), baseDir);
        writeToFile("script.js", result.getJsCode(), baseDir);
        writeToFile("style.css", result.getCssCode(), baseDir);
    }

    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        if (StrUtil.isBlank(result.getHtmlCode())){
            //html代码必须有
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"html内容为空");
        }
    }
}
