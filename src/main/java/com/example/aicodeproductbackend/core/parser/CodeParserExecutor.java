package com.example.aicodeproductbackend.core.parser;

import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {
   private static final HtmlCodeParser htmlCodeParser= new HtmlCodeParser();
   public static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * 根据类型执行解析
     * @param codeContext  代码内容
     * @param codeGenTypeEnum 代码类型
     * @return 解析结果
     */
   public static Object executorParser(String codeContext, CodeGenTypeEnum codeGenTypeEnum){
       return switch (codeGenTypeEnum){
           case HTML -> htmlCodeParser.parseCode(codeContext);
           case MULTI_FILE -> multiFileCodeParser.parseCode(codeContext);
           default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码类型："+codeGenTypeEnum);
       };
   }
}
