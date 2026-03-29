package com.example.aicodeproductbackend.core;

import com.example.aicodeproductbackend.ai.model.HtmlCodeResult;
import com.example.aicodeproductbackend.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {
    @Test
    void parseHtmlCode() {
        String codeContent ="这是一些文本\n" +
                "```html\n" +
                "<div class='container'>\n" +
                "    <h1>标题</h1>\n" +
                "    <p>段落内容</p>\n" +
                "</div>\n" +
                "```\n" +
                "更多文本\n" +
                "```html\n" +
                "<span>第二个代码块</span>\n" +
                "```";
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：
                html 格式
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>

                css 格式
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                ```js
                console.log('页面加载完成');

                文件创建完成！
                """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
    }





    @Test
    void regionMarth() {
            // 定义正则表达式
            String regex = "```html\\s*\\n([\\s\\S]*?)```";

            // 测试文本（Markdown格式）
            String markdown =
                    "这是一些文本\n" +
                            "```html\n" +
                            "<div class='container'>\n" +
                            "    <h1>标题</h1>\n" +
                            "    <p>段落内容</p>\n" +
                            "</div>\n" +
                            "```\n" +
                            "更多文本\n" +
                            "```html\n" +
                            "<span>第二个代码块</span>\n" +
                            "```";

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(markdown);

            int count = 1;
            while (matcher.find()) {
                System.out.println("代码块 " + count + ":");
                System.out.println("匹配到的完整内容:");
                System.out.println(matcher.group(0)); // 完整匹配
                System.out.println("\n提取的HTML内容:");
                System.out.println(matcher.group(1)); // 捕获组内容
                System.out.println("-----------------------------------");
                count++;
            }
        }
}
