// HTTP节点测试脚本
// 直接测试HTTP请求功能，验证HTTP节点的实现

const testHttpNode = async () => {
  console.log('=== HTTP节点测试开始 ===\n');

  // 测试配置
  const testCases = [
    {
      name: '测试1: GET请求 - 获取单个帖子',
      config: {
        httpUrl: 'https://jsonplaceholder.typicode.com/posts/1',
        httpMethod: 'GET',
        httpHeaders: { 'Content-Type': 'application/json' },
        httpBody: '',
        httpOutputKey: 'httpResult'
      },
      expectedFields: ['userId', 'id', 'title', 'body']
    },
    {
      name: '测试2: GET请求 - 获取用户列表',
      config: {
        httpUrl: 'https://jsonplaceholder.typicode.com/users',
        httpMethod: 'GET',
        httpHeaders: { 'Content-Type': 'application/json' },
        httpBody: '',
        httpOutputKey: 'httpResult'
      },
      expectedFields: ['id', 'name', 'email', 'username']
    },
    {
      name: '测试3: POST请求 - 创建新帖子',
      config: {
        httpUrl: 'https://jsonplaceholder.typicode.com/posts',
        httpMethod: 'POST',
        httpHeaders: { 'Content-Type': 'application/json' },
        httpBody: JSON.stringify({
          title: 'foo',
          body: 'bar',
          userId: 1
        }),
        httpOutputKey: 'httpResult'
      },
      expectedFields: ['id', 'title', 'body', 'userId']
    }
  ];

  // 执行测试
  for (const testCase of testCases) {
    console.log(`\n--- ${testCase.name} ---`);
    console.log('配置:', JSON.stringify(testCase.config, null, 2));

    try {
      const startTime = Date.now();

      // 模拟HTTP节点执行
      const response = await fetch(testCase.config.httpUrl, {
        method: testCase.config.httpMethod,
        headers: testCase.config.httpHeaders,
        body: testCase.config.httpMethod !== 'GET' && testCase.config.httpBody ? testCase.config.httpBody : undefined
      });

      const endTime = Date.now();
      const duration = endTime - startTime;

      console.log(`状态码: ${response.status} ${response.statusText}`);
      console.log(`响应时间: ${duration}ms`);

      if (!response.ok) {
        console.error(`请求失败: ${response.status}`);
        continue;
      }

      const data = await response.json();
      console.log('响应数据:', JSON.stringify(data, null, 2));

      // 验证响应字段
      const dataArray = Array.isArray(data) ? data : [data];
      const firstItem = dataArray[0];

      if (firstItem) {
        const missingFields = testCase.expectedFields.filter(field => !(field in firstItem));
        if (missingFields.length > 0) {
          console.warn(`⚠️  缺少预期字段: ${missingFields.join(', ')}`);
        } else {
          console.log('✅ 所有预期字段都存在');
        }
      }

      // 模拟输出变量
      const output = {
        [testCase.config.httpOutputKey]: data
      };
      console.log(`输出变量: ${testCase.config.httpOutputKey} =`, JSON.stringify(output, null, 2));

    } catch (error) {
      console.error('❌ 请求失败:', error.message);
    }
  }

  console.log('\n=== HTTP节点测试完成 ===');
};

// 在浏览器环境中运行
if (typeof window !== 'undefined') {
  window.testHttpNode = testHttpNode;
  console.log('测试函数已加载，在控制台运行 testHttpNode() 开始测试');
} else {
  // 在Node.js环境中运行
  testHttpNode().catch(console.error);
}
