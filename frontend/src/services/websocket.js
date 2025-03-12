// WebSocket通信服务配置

class WebSocketService {
    constructor() {
        this.ws = null;
        this.isConnected = false;
        this.onMessageCallback = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000; // 重连延迟时间（毫秒）
        this.connectionTimeout = 5000; // 连接超时时间（毫秒）
    }

    connect() {
        return new Promise((resolve, reject) => {
            try {
                // 设置连接超时处理
                const timeoutId = setTimeout(() => {
                    if (this.ws && this.ws.readyState === WebSocket.CONNECTING) {
                        this.ws.close();
                        reject(new Error('WebSocket连接超时'));
                    }
                }, this.connectionTimeout);

                // 连接到后端WebSocket服务器
                this.ws = new WebSocket('ws://localhost:8080/speech');

                this.ws.onopen = () => {
                    clearTimeout(timeoutId);
                    console.log('WebSocket连接已建立');
                    this.isConnected = true;
                    this.reconnectAttempts = 0;
                    resolve();
                };

                this.ws.onclose = () => {
                    console.log('WebSocket连接已关闭');
                    this.isConnected = false;
                    this.handleReconnect();
                };

                this.ws.onerror = (error) => {
                    console.error('WebSocket连接错误:', error);
                    this.isConnected = false;
                    reject(error);
                };

                this.ws.onmessage = (event) => {
                    // 处理心跳消息
                    if (event.data === 'ping') {
                        console.log('收到心跳ping，发送pong响应');
                        this.ws.send('pong');
                        return;
                    }
                    
                    if (this.onMessageCallback) {
                        this.onMessageCallback(event.data);
                    }
                };
            } catch (error) {
                console.error('创建WebSocket连接时出错:', error);
                reject(error);
            }
        });
    }

    handleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            setTimeout(() => {
                this.connect().catch(error => {
                    console.error('重连失败:', error);
                });
            }, this.reconnectDelay);
        } else {
            console.error('达到最大重连次数，停止重连');
        }
    }

    disconnect() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
            this.isConnected = false;
            this.reconnectAttempts = 0;
        }
    }

    // 发送音频数据到服务器
    sendAudioData(audioData) {
        if (this.ws && this.isConnected) {
            try {
                this.ws.send(audioData);
            } catch (error) {
                console.error('发送音频数据时出错:', error);
                throw error;
            }
        } else {
            throw new Error('WebSocket未连接');
        }
    }

    // 设置接收消息的回调函数
    setOnMessageCallback(callback) {
        this.onMessageCallback = callback;
    }

    // 检查连接状态
    isWebSocketConnected() {
        return this.isConnected;
    }
}

// 导出WebSocket服务实例
const webSocketService = new WebSocketService();
export default webSocketService;