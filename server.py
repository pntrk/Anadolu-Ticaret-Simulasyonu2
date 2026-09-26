import http.server
import socketserver
import os
import glob

PORT = 3000

class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        try:
            apk_files = glob.glob("Anadolu_Ticaret_Simulasyonu_*.apk")
            latest_apk = apk_files[0] if apk_files else ""
            latest_aab = "Anadolu_Ticaret_Simulasyonu_LATEST.aab"
            
            apk_size = f"{os.path.getsize(latest_apk) / (1024*1024):.2f} MB" if latest_apk and os.path.exists(latest_apk) else "Bulunamadı"
            aab_size = f"{os.path.getsize(latest_aab) / (1024*1024):.2f} MB" if os.path.exists(latest_aab) else "Bulunamadı"

            if self.path == "/" or self.path == "/index.html":
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                
                with open("index.html", "r", encoding="utf-8") as f:
                    html_content = f.read()
                
                html_rendered = html_content.replace("{latest_apk}", latest_apk).replace("{apk_size}", apk_size).replace("{aab_size}", aab_size)
                self.wfile.write(html_rendered.encode("utf-8"))
                return

            elif self.path == "/Anadolu_Ticaret_Simulasyonu_LATEST.aab":
                if os.path.exists(latest_aab):
                    self.send_response(200)
                    self.send_header("Content-Type", "application/octet-stream")
                    self.send_header("Content-Length", str(os.path.getsize(latest_aab)))
                    self.send_header("Content-Disposition", 'attachment; filename="Anadolu_Ticaret_Simulasyonu_LATEST.aab"' )
                    self.send_header("X-Accel-Buffering", "no")
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()
                    with open(latest_aab, "rb") as f:
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
                    self.send_header("Content-Type", "application/vnd.android.package-archive")
                    self.send_header("Content-Length", str(os.path.getsize(latest_apk)))
                    self.send_header("Content-Disposition", f'attachment; filename="{latest_apk}"')
                    self.send_header("X-Accel-Buffering", "no")
                    self.send_header("Access-Control-Allow-Origin", "*")
                    self.end_headers()
                    with open(latest_apk, "rb") as f:
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
