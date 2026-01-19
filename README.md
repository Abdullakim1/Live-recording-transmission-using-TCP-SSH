# Live Recording transmission via TCP and SSH


## Purpose
This tool turns an Android device into a remote microphone.
* **Low Latency:** Uses raw TCP sockets without heavy HTTP/RTSP overhead.
* **Direct Playback:** Pipes audio directly to the Linux audio subsystem (PulseAudio/PipeWire) for immediate listening.

---

## Working Principle

### 1. The Architecture
The system operates on a **Client-Server** model:
* **Client (Android Phone):** Captures raw PCM audio data from the hardware microphone and pushes it immediately to a TCP socket.
* **Server (Laptop):** Listens on a specific port (e.g., `9000`), receives the raw byte stream, and pipes it directly to the sound card.

### 2. Audio Format
To ensure zero lag, no encoding (MP3/AAC) is used. The stream is **Raw PCM**:
* **Rate:** 44,100 Hz
* **Depth:** 16-bit (S16_LE)
* **Channels:** Mono

---

ssh -p 443 -R0:localhost:9000 a.pinggy.io creates SSH tunnel
ssh -p 443 -R0:localhost:9000 tcp@a.pinggy.io Pinggy assigns a public TCP port
nc -l -p 9000 | pacat --format=s16le --rate=44100 --channels=1  for paulse audio
adb shell pm grant com.example.test android.permission.RECORD_AUDIO  permission grant in terminal
adb shell am start -n com.example.test/.MainActivity wake the app
