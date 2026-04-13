package com.example.aicodeproductbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aicodeproductbackend.ai.AiCodeGenTypeRoutingService;
import com.example.aicodeproductbackend.ai.AiCodeGenTypeRoutingServiceFactory;
import com.example.aicodeproductbackend.contant.AppConstant;
import com.example.aicodeproductbackend.core.AiCodeGeneratorFacade;
import com.example.aicodeproductbackend.core.builder.VueProjectBuilder;
import com.example.aicodeproductbackend.core.handler.StreamHandlerExecutor;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.exception.ThrowUtils;
import com.example.aicodeproductbackend.model.dto.app.AppAddRequest;
import com.example.aicodeproductbackend.model.dto.app.AppQueryRequest;
import com.example.aicodeproductbackend.model.entity.App;
import com.example.aicodeproductbackend.model.entity.User;
import com.example.aicodeproductbackend.model.enums.CodeGenTypeEnum;
import com.example.aicodeproductbackend.model.vo.AppVO;
import com.example.aicodeproductbackend.model.vo.UserVO;
import com.example.aicodeproductbackend.service.ChatHistoryService;
import com.example.aicodeproductbackend.service.ScreenshotService;
import com.example.aicodeproductbackend.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.aicodeproductbackend.mapper.AppMapper;
import com.example.aicodeproductbackend.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 应用 服务层实现。
 *
 * @author Fairytail
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    private UserService userService;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private VueProjectBuilder vueProjectBuilder;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser){
        ThrowUtils.throwIf(appId == null||appId<=0, ErrorCode.PARAMS_ERROR,"appId不能为空");
        ThrowUtils.throwIf(message== null||message.length()<=0, ErrorCode.PARAMS_ERROR,"message不能为空");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR,"appId不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限访问该应用");
        }
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的代码生成类型");
        }
        //添加用户消息到对话历史
        chatHistoryService.saveUserMessage(appId, loginUser.getId(), message);
        //调用 AI 生成代码（流式）
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        //收集 AI 响应内容并在完成后记录到对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        if (!(id instanceof Long appId)) {
            return false;
        }
        chatHistoryService.removeByAppId(appId);
        return super.removeById(id);
    }


    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        //使用智能选择代码工程创建多例service实例
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }


    @Override
    public String  deployApp(Long appId, User loginUser) {
        //1.校验参数
        ThrowUtils.throwIf(appId == null||appId<=0, ErrorCode.PARAMS_ERROR,"appId不能为空");
        ThrowUtils.throwIf(loginUser == null||loginUser.getId() == null, ErrorCode.PARAMS_ERROR,"用户未登录");
        //2.查询应用信息
        App app = this.getById(appId);
        //3.校验用户权限
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限部署该应用");
        }
        //4.检查是否有deployKey,没有就生成6位随机数
            String deployKey = app.getDeployKey();
            if (StrUtil.isBlank(deployKey)) {
               deployKey= RandomUtil.randomString(6);
            }
        //5.获取代码生成类型，构建源目录路径
            String codeGenType = app.getCodeGenType();
            String sourceDirname = codeGenType + "_" + appId;
            String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirname;
        //6.检查源码目录是否存在，不存在则抛出异常
            File file = new File(sourceDirPath);
            if (!file.exists()||!file.isDirectory()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"源码目录不存在");
            }
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
            if (CodeGenTypeEnum.VUE_PROJECT.equals(codeGenTypeEnum)){
            boolean buildResult = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildResult, ErrorCode.SYSTEM_ERROR, "构建vue项目失败");
            //检查dist目录
                File dist = new File(sourceDirPath, "dist");
                ThrowUtils.throwIf(!dist.exists()||!dist.isDirectory(), ErrorCode.SYSTEM_ERROR, "dist目录不存在");
                //将dist目录作为部署源地址
                file=dist;
            }
        //7.复制源目录到部署目录
            String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
            try {
                FileUtil.copyContent(file, new File(deployDirPath), true);
            }catch (Exception e){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"部署目录失败："+e.getMessage());
            }

            //8.更新应用信息
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            boolean updateResult = this.updateById(updateApp);
            if (!updateResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新应用信息失败");
            }
        // 9. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 10. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;
    }
    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }


    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }




}
