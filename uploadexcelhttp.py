import tkinter as tk
from tkinter import messagebox, simpledialog, scrolledtext
import json
import os
import sys
import threading
import time
from datetime import datetime
import requests
import zipfile
import olefile

DEFAULT_PASSWORD = "123456"
stop_event = threading.Event()

def get_app_dir():
    if getattr(sys, 'frozen', False):
        return os.path.dirname(sys.executable)
    else:
        return os.path.dirname(os.path.abspath(__file__))

CONFIG_PATH = os.path.join(get_app_dir(), "config.json")
LOG_PATH = os.path.join(get_app_dir(), "log.txt")

def ensure_config():
    if not os.path.exists(CONFIG_PATH):
        default_config = {
            "api_url": "http://127.0.0.1:8080/upload/upload_excel",
            "excel_dir": r"D:\excel",
            "upload_interval_min": 1,
            "process_name": "文件名称",
            "password": DEFAULT_PASSWORD   # ← 默认密码
        }
        with open(CONFIG_PATH, "w", encoding="utf-8") as f:
            json.dump(default_config, f, indent=4, ensure_ascii=False)
def get_password():
    """从配置文件读取密码，没有就用默认值"""
    config = load_config()
    return config.get("password", DEFAULT_PASSWORD)


def load_config():
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

def save_config(new_config):
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(new_config, f, indent=4, ensure_ascii=False)

def log_message(msg):
    timestamp = datetime.now().strftime("[%Y-%m-%d %H:%M:%S] ")
    full_msg = timestamp + msg
    print(full_msg)
    if log_window:
        log_window.insert(tk.END, full_msg + "\n")
        log_window.see(tk.END)
    with open(LOG_PATH, "a", encoding="utf-8") as log_file:
        log_file.write(full_msg + "\n")


import zipfile
import olefile


def excel_has_images(file_path):
    ext = os.path.splitext(file_path)[1].lower()

    # CSV 不可能有嵌入图片
    if ext == ".csv":
        return False

    try:
        # 1. 检测 xlsx/xlsm/xlsb
        if ext in [".xlsx", ".xlsm", ".xlsb"]:
            with zipfile.ZipFile(file_path, 'r') as z:
                for name in z.namelist():
                    if name.startswith("xl/drawings/") or name.startswith("xl/media/"):
                        return True

        # 2. 检测 xls（OLE Compound Document 格式）
        elif ext == ".xls":
            if olefile.isOleFile(file_path):
                with olefile.OleFileIO(file_path) as ole:
                    # 常见图片数据会在这些流中
                    for stream_name in ole.listdir():
                        stream_path = "/".join(stream_name)
                        if "MBD" in stream_path or "Pictures" in stream_path or "Drawing" in stream_path:
                            return True
                    # 额外检测二进制中是否有 JPEG/PNG 特征字节
                    with open(file_path, "rb") as f:
                        content = f.read()
                        if b"\xFF\xD8\xFF" in content or b"\x89PNG" in content:
                            return True
    except Exception as e:
        log_message(f"⚠️ 无法检测图片: {file_path}, 错误: {e}")

    return False


def upload_excel_http(file_path, config, relative_path=None):
    log_message(f"📡 尝试上传文件: {file_path} 到 {config['api_url']}")
    try:
        if not os.path.exists(file_path):
            log_message(f"❌ 文件不存在: {file_path}")
            return False

        file_size = os.path.getsize(file_path)
        if file_size == 0:
            log_message(f"⚠️ 警告: 文件是空的: {file_path}")
            return False

        api_url = config["api_url"]

        ext = os.path.splitext(file_path)[1].lower()
        content_types = {
            ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            ".xls": "application/vnd.ms-excel",
            ".xlsm": "application/vnd.ms-excel.sheet.macroEnabled.12",
            ".xlsb": "application/vnd.ms-excel.sheet.binary.macroEnabled.12",
            ".csv": "text/csv"
        }
        content_type = content_types.get(ext, "application/octet-stream")

        with open(file_path, 'rb') as f:
            files = {
                'file': (os.path.basename(file_path), f, content_type)
            }
            data = {
                'process_name': config.get("process_name", "未知文件名称"),
                'relative_path': relative_path or os.path.basename(file_path)
            }
            response = requests.post(api_url, files=files, data=data)

        if response.status_code == 200:
            log_message(f"✅ 上传成功: {file_path}")
            return True
        else:
            log_message(f"❌ 上传失败: {file_path}, 状态码: {response.status_code}, 返回: {response.text}")
            return False

    except Exception as e:
        log_message(f"❌ 上传失败: {file_path}, 错误: {str(e)}")
        return False



def scan_and_upload():
    config = load_config()
    excel_dir = config["excel_dir"]

    if not os.path.exists(excel_dir):
        log_message(f"❌ 目录不存在: {excel_dir}")
        return

    record_path = os.path.join(get_app_dir(), "upload_record.json")

    if os.path.exists(record_path):
        with open(record_path, "r", encoding="utf-8") as f:
            upload_record = json.load(f)
    else:
        upload_record = {}

    updated_record = upload_record.copy()

    for root, dirs, files in os.walk(excel_dir):
        log_message(f"📂 正在扫描目录: {root}")
        for file in files:
            if file.lower().endswith((".xlsx", ".xls", ".xlsm", ".xlsb", ".csv")):
                file_path = os.path.join(root, file)
                relative_path = os.path.relpath(file_path, excel_dir).replace("\\", "/")
                log_message(f"📄 发现文件: {relative_path}，准备上传...")

                last_modified = os.path.getmtime(file_path)

                if file_path not in upload_record or upload_record[file_path] < last_modified:
                    if excel_has_images(file_path):
                        log_message(f"❌ 文件含有图片，禁止上传: {relative_path}")
                        continue
                    success = upload_excel_http(file_path, config, relative_path)
                    if success:
                        updated_record[file_path] = last_modified
                    else:
                        log_message(f"⚠️ 文件未上传成功，记录不更新: {file_path}")
                else:
                    log_message(f"⏩ 已上传且未更新: {file_path}")
            else:
                log_message(f"⏩ 非Excel文件，跳过: {file}")
    with open(record_path, "w", encoding="utf-8") as f:
        json.dump(updated_record, f, indent=4, ensure_ascii=False)

    log_message("✅ 本轮扫描完成，所有目录均已处理")



def show_config_window():
    pwd = simpledialog.askstring("密码验证", "请输入管理密码：", show="*")
    if pwd != get_password():
        messagebox.showerror("错误", "密码错误！")
        return

    config = load_config()
    config_win = tk.Toplevel()
    config_win.title("修改配置")
    config_win.geometry("580x400")

    labels = [
        "API接口地址：",
        "本地Excel目录（Windows格式）：",
        "上传间隔（分钟）：",
        "文件名称：",
        "修改密码："   # 新增密码输入框
    ]
    keys = ["api_url", "excel_dir", "upload_interval_min", "process_name", "password"]
    entries = {}

    for i, label_text in enumerate(labels):
        label = tk.Label(config_win, text=label_text)
        label.grid(row=i, column=0, sticky="e", padx=5, pady=5)

        if keys[i] == "password":
            entry = tk.Entry(config_win, width=35, show="*")  # 密码输入隐藏
            entry.insert(0, str(config.get(keys[i], "")))
        else:
            entry = tk.Entry(config_win, width=35)
            entry.insert(0, str(config.get(keys[i], "")))
        entry.grid(row=i, column=1, padx=5, pady=5)
        entries[keys[i]] = entry

    def save_and_close():
        for key in keys:
            value = entries[key].get().strip()
            if key == "upload_interval_min":
                try:
                    value = int(value)
                except:
                    messagebox.showerror("错误", f"{key} 必须是数字！", parent=config_win)
                    return
            config[key] = value
        save_config(config)
        messagebox.showinfo("成功", "配置已保存！", parent=config_win)
        config_win.destroy()

    save_button = tk.Button(config_win, text="保存配置", command=save_and_close, height=2, width=20)
    save_button.grid(row=len(labels), column=0, columnspan=2, pady=15)

def auto_upload_thread():
    while not stop_event.is_set():
        scan_and_upload()
        config = load_config()
        interval = config.get("upload_interval_min", 1)
        for _ in range(interval * 60):
            if stop_event.is_set():
                break
            time.sleep(1)

def start_auto_upload():
    if stop_event.is_set():
        stop_event.clear()
    threading.Thread(target=auto_upload_thread, daemon=True).start()
    log_message("✅ 自动上传线程已启动")

def stop_auto_upload():
    stop_event.set()
    log_message("🛑 自动上传线程已停止")

def start_gui():
    global log_window

    window = tk.Tk()
    window.title("Excel HTTP自动上传工具")
    window.geometry("800x580")

    start_btn = tk.Button(window, text="开始自动上传", command=start_auto_upload, height=2, width=30)
    start_btn.pack(pady=5)

    stop_btn = tk.Button(window, text="停止自动上传", command=stop_auto_upload, height=2, width=30)
    stop_btn.pack(pady=5)

    modify_btn = tk.Button(window, text="修改配置", command=show_config_window, height=2, width=30)
    modify_btn.pack(pady=5)

    log_window = scrolledtext.ScrolledText(window, height=25, width=80)
    log_window.pack(padx=10, pady=10)
    log_window.insert(tk.END, "✅ 日志窗口已启动...\n")
    log_window.see(tk.END)

    window.mainloop()

if __name__ == "__main__":
    log_window = None
    ensure_config()
    start_gui()