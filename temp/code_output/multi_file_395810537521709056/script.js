// 等待DOM完全加载后执行
document.addEventListener('DOMContentLoaded', function() {
    // 获取DOM元素
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('togglePassword');
    const submitBtn = document.getElementById('submitBtn');
    const spinner = document.getElementById('spinner');
    const btnText = document.querySelector('.btn-text');
    const successMessage = document.getElementById('successMessage');
    const usernameError = document.getElementById('usernameError');
    const passwordError = document.getElementById('passwordError');
    const registerLink = document.getElementById('registerLink');
    
    // 密码显示/隐藏切换
    togglePasswordBtn.addEventListener('click', function() {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        // 更新按钮文本
        const eyeIcon = this.querySelector('.eye-icon');
        eyeIcon.textContent = type === 'password' ? '👁️' : '🙈';
    });
    
    // 用户名实时验证
    usernameInput.addEventListener('input', function() {
        validateUsername();
    });
    
    // 密码实时验证
    passwordInput.addEventListener('input', function() {
        validatePassword();
    });
    
    // 用户名验证函数
    function validateUsername() {
        const username = usernameInput.value.trim();
        usernameError.textContent = '';
        
        if (username.length === 0) {
            usernameError.textContent = '用户名或邮箱不能为空';
            return false;
        }
        
        // 检查是否为邮箱格式
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        // 检查是否为有效的用户名（字母数字和下划线，3-20个字符）
        const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
        
        if (!emailRegex.test(username) && !usernameRegex.test(username)) {
            usernameError.textContent = '请输入有效的用户名或邮箱地址';
            return false;
        }
        
        return true;
    }
    
    // 密码验证函数
    function validatePassword() {
        const password = passwordInput.value;
        passwordError.textContent = '';
        
        if (password.length === 0) {
            passwordError.textContent = '密码不能为空';
            return false;
        }
        
        if (password.length < 6) {
            passwordError.textContent = '密码长度至少为6个字符';
            return false;
        }
        
        return true;
    }
    
    // 表单提交处理
    loginForm.addEventListener('submit', function(event) {
        event.preventDefault();
        
        // 验证表单
        const isUsernameValid = validateUsername();
        const isPasswordValid = validatePassword();
        
        if (!isUsernameValid || !isPasswordValid) {
            // 如果有错误，聚焦到第一个错误字段
            if (!isUsernameValid) {
                usernameInput.focus();
            } else if (!isPasswordValid) {
                passwordInput.focus();
            }
            return;
        }
        
        // 禁用提交按钮并显示加载动画
        submitBtn.disabled = true;
        btnText.style.opacity = '0';
        spinner.style.display = 'block';
        
        // 模拟API调用延迟
        setTimeout(function() {
            // 在实际应用中，这里应该是真实的API调用
            // 模拟登录成功
            showSuccessMessage();
            
            // 在实际应用中，这里应该是重定向到仪表板或其他页面
            // 例如：window.location.href = '/dashboard';
        }, 1500);
    });
    
    // 显示成功消息
    function showSuccessMessage() {
        // 隐藏表单，显示成功消息
        loginForm.style.display = 'none';
        successMessage.style.display = 'block';
        
        // 模拟跳转延迟
        setTimeout(function() {
            alert('登录成功！在实际应用中，这里会跳转到用户仪表板。');
            
            // 重置表单状态（在实际应用中可能不需要）
            resetForm();
        }, 2000);
    }
    
    // 重置表单状态
    function resetForm() {
        // 重新显示表单，隐藏成功消息
        loginForm.style.display = 'block';
        successMessage.style.display = 'none';
        
        // 重置按钮状态
        submitBtn.disabled = false;
        btnText.style.opacity = '1';
        spinner.style.display = 'none';
        
        // 清除表单数据（可选）
        // loginForm.reset();
        // usernameError.textContent = '';
        // passwordError.textContent = '';
    }
    
    // 注册链接点击事件
    registerLink.addEventListener('click', function(event) {
        event.preventDefault();
        alert('注册功能正在开发中...');
    });
    
    // 忘记密码链接点击事件
    document.querySelector('.forgot-password').addEventListener('click', function(event) {
        event.preventDefault();
        alert('密码重置功能正在开发中...');
    });
    
    // 页脚链接点击事件
    document.querySelectorAll('.footer-links a').forEach(link => {
        link.addEventListener('click', function(event) {
            event.preventDefault();
            alert('此功能正在开发中...');
        });
    });
    
    // 添加键盘快捷键支持
    document.addEventListener('keydown', function(event) {
        // Ctrl+Enter 提交表单
        if (event.ctrlKey && event.key === 'Enter') {
            if (!submitBtn.disabled) {
                loginForm.dispatchEvent(new Event('submit'));
            }
        }
        
        // Esc 键清除表单
        if (event.key === 'Escape') {
            loginForm.reset();
            usernameError.textContent = '';
            passwordError.textContent = '';
        }
    });
    
    // 初始焦点设置
    usernameInput.focus();
});