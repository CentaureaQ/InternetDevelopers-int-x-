import json
import requests
import time

# 后端API地址
BASE_URL = "http://localhost:8080/api"

# 创建工作流
print("=== 创建工作流 ===")

# 读取测试工作流配置
with open("test_parallel_loop_workflow.json", "r", encoding="utf-8") as f:
    workflow_data = json.load(f)

# 创建工作流请求体
create_request = {
    "name": "测试并行循环工作流",
    "description": "测试并行和循环节点功能",
    "nodes": workflow_data["nodes"],
    "edges": workflow_data["edges"]
}

# 发送创建请求
response = requests.post(f"{BASE_URL}/workflows", json=create_request)
if response.status_code != 200:
    print(f"创建工作流失败: {response.status_code} {response.text}")
    exit()

workflow_id = response.json()["data"]
print(f"工作流创建成功，ID: {workflow_id}")

# 等待几秒，确保工作流已保存
print("等待工作流保存...")
time.sleep(3)

# 调试工作流
print("\n=== 调试工作流 ===")

# 调试请求体
debug_request = {
    "inputs": {
        "query": "测试查询"
    }
}

# 发送调试请求
response = requests.post(f"{BASE_URL}/workflows/{workflow_id}/debug", json=debug_request)
if response.status_code != 200:
    print(f"调试工作流失败: {response.status_code} {response.text}")
    exit()

# 解析并打印调试结果
result = response.json()
print(f"调试状态: {result['status']}")
print(f"调试消息: {result['message']}")

if "data" in result:
    data = result["data"]
    print("\n调试结果:")
    print(f"  输出: {json.dumps(data.get('output'), ensure_ascii=False, indent=2)}")
    print(f"  变量: {json.dumps(data.get('vars'), ensure_ascii=False, indent=2)}")
    print(f"  轨迹: {json.dumps(data.get('trace'), ensure_ascii=False, indent=2)}")

print("\n=== 测试完成 ===")