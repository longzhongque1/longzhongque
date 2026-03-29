// 登录表单处理
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const rememberCheckbox = document.getElementById('remember');
    const loginMessage = document.getElementById('loginMessage');
    
    // 从本地存储加载记住的用户名
    if (localStorage.getItem('rememberUser') === 'true') {
        const savedUsername = localStorage.getItem('username');
        if (savedUsername) {
            usernameInput.value = savedUsername;
            rememberCheckbox.checked = true;
        }
    }
    
    // 表单提交事件
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // 清除之前的错误和消息
        clearErrors();
        hideMessage();
        
        // 验证输入
        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();
        let isValid = true;
        
        // 用户名验证
        if (!username) {
            showError('usernameError', '请输入用户名');
            usernameInput.classList.add('error');
            isValid = false;
        } else if (username.length < 3) {
            showError('usernameError', '用户名至少3个字符');
            usernameInput.classList.add('error');
            isValid = false;
        }
        
        // 密码验证
        if (!password) {
            showError('passwordError', '请输入密码');
            passwordInput.classList.add('error');
            isValid = false;
        } else if (password.length < 6) {
            showError('passwordError', '密码至少6个字符');
            passwordInput.classList.add('error');
            isValid = false;
        }
        
        // 如果验证通过，模拟登录
        if (isValid) {
            // 保存记住的用户名
            if (rememberCheckbox.checked) {
                localStorage.setItem('rememberUser', 'true');
                localStorage.setItem('username', username);
            } else {
                localStorage.removeItem('rememberUser');
                localStorage.removeItem('username');
            }
            
            // 模拟登录成功
            showMessage('登录成功！正在跳转...', 'success');
            
            // 在实际应用中，这里会发送请求到服务器
            console.log('登录信息:', { username, password });
            
            // 重置表单
            setTimeout(() => {
                loginForm.reset();
                hideMessage();
            }, 2000);
        }
    });
    
    // 实时验证
    usernameInput.addEventListener('input', function() {
        if (this.value.trim()) {
            this.classList.remove('error');
            hideError('usernameError');
        }
    });
    
    passwordInput.addEventListener('input', function() {
        if (this.value.trim()) {
            this.classList.remove('error');
            hideError('passwordError');
        }
    });
    
    // 辅助函数
    function showError(elementId, message) {
        const element = document.getElementById(elementId);
        element.textContent = message;
    }
    
    function hideError(elementId) {
        const element = document.getElementById(elementId);
        element.textContent = '';
    }
    
    function showMessage(message, type) {
        loginMessage.textContent = message;
        loginMessage.className = `login-message ${type}`;
    }
    
    function hideMessage() {
        loginMessage.style.display = 'none';
    }
    
    function clearErrors() {
        hideError('usernameError');
        hideError('passwordError');
        usernameInput.classList.remove('error');
        passwordInput.classList.remove('error');
    }
    
    // 忘记密码和注册链接点击事件
    document.querySelector('.forgot-password').addEventListener('click', function(e) {
        e.preventDefault();
        showMessage('请联系管理员重置密码', 'error');
    });
    
    document.querySelector('.register-link').addEventListener('click', function(e) {
        e.preventDefault();
        showMessage('注册功能正在开发中', 'error');
    });
});