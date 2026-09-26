import http.server
import socketserver
import os
import glob

PORT = 3000

HTML_TEMPLATE = """<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Anadolu Ticaret Simülasyonu - İndirme Merkezi</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Plus Jakarta Sans', sans-serif;
        }
        body {
            background-color: #f3f4f6;
            color: #1f2937;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 20px;
        }
        .card {
            background: white;
            padding: 40px;
            border-radius: 24px;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
            max-width: 600px;
            width: 100%;
            text-align: center;
        }
        .logo-placeholder {
            width: 80px;
            height: 80px;
            background: #4f46e5;
            color: white;
            font-size: 32px;
            font-weight: bold;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 20px;
            margin: 0 auto 24px;
        }
        h1 {
            font-size: 24px;
            font-weight: 700;
            color: #111827;
            margin-bottom: 12px;
        }
        p.subtitle {
            color: #6b7280;
            font-size: 14px;
            margin-bottom: 32px;
            line-height: 1.5;
        }
        .file-section {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 16px;
            padding: 20px;
            margin-bottom: 20px;
            text-align: left;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .file-info {
            flex: 1;
        }
        .file-title {
            font-weight: 600;
            color: #111827;
            font-size: 16px;
            margin-bottom: 4px;
        }
        .file-meta {
            color: #6b7280;
            font-size: 13px;
        }
        .download-btn {
            background: #4f46e5;
            color: white;
            border: none;
            cursor: pointer;
            padding: 12px 24px;
            border-radius: 12px;
            font-weight: 600;
            font-size: 14px;
            transition: background 0.2s, opacity 0.2s;
            display: inline-block;
            text-align: center;
            min-width: 140px;
        }
        .download-btn:hover {
            background: #4338ca;
        }
        .download-btn.aab-btn {
            background: #059669;
        }
        .download-btn.aab-btn:hover {
            background: #047857;
        }
        .info-box {
            background: #eff6ff;
            border: 1px solid #bfdbfe;
            border-radius: 12px;
            padding: 16px;
            margin-top: 24px;
            text-align: left;
            font-size: 13px;
            color: #1e40af;
            line-height: 1.6;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="logo-placeholder">AT</div>
        <h1>Anadolu Ticaret Simülasyonu</h1>
        <p class="subtitle">Google Play Console ve cihaz testleriniz için doğrudan yüksek hızlı indirme paneli. Tarayıcınız üzerinden doğrudan %100 orijinal ve tam boyutlu paketleri indirebilirsiniz.</p>
        
        <div class="file-section">
            <div class="file-info">
                <div class="file-title">Google Play Dağıtım Paketi (AAB)</div>
                <div class="file-meta">Dosya: Anadolu_Ticaret_Simulasyonu_LATEST.aab<br>Boyut: {aab_size}</div>
            </div>
            <button id="aab-btn" onclick="downloadFile('/Anadolu_Ticaret_Simulasyonu_LATEST.aab', 'Anadolu_Ticaret_Simulasyonu_LATEST.aab', 'aab-btn')" class="download-btn aab-btn">AAB İndir</button>
        </div>

        <div class="file-section">
            <div class="file-info">
                <div class="file-title">Test ve Kurulum Paketi (APK)</div>
                <div class="file-meta">Dosya: {latest_apk}<br>Boyut: {apk_size}</div>
            </div>
            <button id="apk-btn" onclick="downloadFile('/{latest_apk}', '{latest_apk}', 'apk-btn')" class="download-btn">APK İndir</button>
        </div>

        <div class="info-box">
            <strong>Bilgilendirme:</strong> AI Studio arayüzündeki dosya senkronizasyon protokolü (IDE) binary dosyaları tam aktaramadığı için 0 KB sorunu yaşanmaktadır. Burası doğrudan sunucunun diskindeki gerçek dosyayı indiren HTTP servisidir. İndirdiğiniz AAB dosyasını doğrudan Google Play Console v1.25 sürümünüze yükleyebilirsiniz.
        </div>
    </div>

    <script>
        async function downloadFile(url, filename, btnId) {
            const btn = document.getElementById(btnId);
            const originalText = btn.innerText;
            btn.innerText = "İndiriliyor... %0";
            btn.style.pointerEvents = "none";
            btn.style.opacity = "0.7";
            
            try {
                const response = await fetch(url, { credentials: 'same-origin' });
                if (!response.ok) throw new Error("Sunucu hatası: " + response.status);
                
                const contentLength = response.headers.get('content-length');
                if (!contentLength) {
                    const blob = await response.blob();
                    triggerDownload(blob, filename);
                    btn.innerText = originalText;
                    btn.style.pointerEvents = "auto";
                    btn.style.opacity = "1";
                    return;
                }
                
                const total = parseInt(contentLength, 10);
                let loaded = 0;
                
                const reader = response.body.getReader();
                const chunks = [];
                
                while(true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    chunks.push(value);
                    loaded += value.length;
                    const percent = Math.round((loaded / total) * 100);
                    btn.innerText = `İndiriliyor... %${percent}`;
                }
                
                const blob = new Blob(chunks, { type: 'application/octet-stream' });
                triggerDownload(blob, filename);
                
                btn.innerText = "Tamamlandı!";
                setTimeout(() => {
                    btn.innerText = originalText;
                    btn.style.pointerEvents = "auto";
                    btn.style.opacity = "1";
                }, 3000);
                
            } catch (error) {
                alert("İndirme başarısız: " + error.message);
                btn.innerText = "Hata Oluştu!";
                setTimeout(() => {
                    btn.innerText = originalText;
                    btn.style.pointerEvents = "auto";
                    btn.style.opacity = "1";
                }, 3000);
            }
        }

        function triggerDownload(blob, filename) {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        }
    </script>
</body>
</html>
"""

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        try:
            apk_files = glob.glob("Anadolu_Ticaret_Simulasyonu_*.apk")
            latest_apk = apk_files[0] if apk_files else ""
            latest_aab = "Anadolu_Ticaret_Simulasyonu_LATEST.aab"
            
            apk_size = f"{os.path.getsize(latest_apk) / (1024*1024):.2f} MB" if latest_apk and os.path.exists(latest_apk) else "Bulunamadı"
            aab_size = f"{os.path.getsize(latest_aab) / (1024*1024):.2f} MB" if os.path.exists(latest_aab) else "Bulunamadı"

            if self.path == '/' or self.path == '/index.html':
                self.send_response(200)
                self.send_header('Content-Type', 'text/html; charset=utf-8')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                
                # Format template
                html_rendered = HTML_TEMPLATE.replace("{latest_apk}", latest_apk).replace("{apk_size}", apk_size).replace("{aab_size}", aab_size)
                self.wfile.write(html_rendered.encode('utf-8'))
                return

            elif self.path == '/Anadolu_Ticaret_Simulasyonu_LATEST.aab':
                if os.path.exists(latest_aab):
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/octet-stream')
                    self.send_header('Content-Length', str(os.path.getsize(latest_aab)))
                    self.send_header('Content-Disposition', 'attachment; filename="Anadolu_Ticaret_Simulasyonu_LATEST.aab"')
                    self.send_header('X-Accel-Buffering', 'no')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    with open(latest_aab, 'rb') as f:
                        while True:
                            chunk = f.read(65536)
                            if not chunk:
                                break
                            self.wfile.write(chunk)
                else:
                    self.send_error(404, "AAB File Not Found")
                return

            elif self.path == f"/{latest_apk}" and latest_apk:
                if os.path.exists(latest_apk):
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/vnd.android.package-archive')
                    self.send_header('Content-Length', str(os.path.getsize(latest_apk)))
                    self.send_header('Content-Disposition', f'attachment; filename="{latest_apk}"')
                    self.send_header('X-Accel-Buffering', 'no')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    with open(latest_apk, 'rb') as f:
                        while True:
                            chunk = f.read(65536)
                            if not chunk:
                                break
                            self.wfile.write(chunk)
                else:
                    self.send_error(404, "APK File Not Found")
                return

            else:
                return super().do_GET()
                
        except (BrokenPipeError, ConnectionResetError) as e:
            print(f"Client disconnected gracefully: {e}")
        except Exception as e:
            print(f"Unexpected request error: {e}")

os.chdir(os.path.dirname(os.path.abspath(__file__)))

class ThreadedHTTPServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

with ThreadedHTTPServer(("", PORT), Handler) as httpd:
    print(f"Serving robustly at port {PORT}")
    httpd.serve_forever()
