package com.example.aicodeproductbackend.core.parser;

public interface CodeParser<T> {
    /**
     * 解析代码
     * @param code
     * @return 解析后的结果对象
     */
    T parseCode (String code);
}
