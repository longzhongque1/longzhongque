package com.example.aicodeproductbackend.core.saver;

import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDir) {
        writeToFile("index.html",result.getHtmlCode(),baseDir );
    }

    @Override
    protected CodeGenTypeEnum getCodeGenType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        if (StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"html内容为空");
        }
    }
}
