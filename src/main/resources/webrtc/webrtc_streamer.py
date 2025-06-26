import asyncio
import json
import cv2
import aiohttp
import aiohttp_cors
from aiohttp import web
from aiortc import RTCPeerConnection, RTCSessionDescription, VideoStreamTrack
from av import VideoFrame

pcs = set()

class CameraStream(VideoStreamTrack):
    def __init__(self):
        super().__init__()
        self.cap = cv2.VideoCapture(0)

    async def recv(self):
        pts, time_base = await self.next_timestamp()
        ret, frame = self.cap.read()
        if not ret:
            raise Exception("Camera read failed")

        frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        video_frame = VideoFrame.from_ndarray(frame, format="rgb24")
        video_frame.pts = pts
        video_frame.time_base = time_base
        return video_frame

async def handle_offer(request):
    try:
        params = await request.json()
        offer = RTCSessionDescription(sdp=params["sdp"], type=params["type"])

        pc = RTCPeerConnection()
        pcs.add(pc)

        @pc.on("connectionstatechange")
        async def on_connectionstatechange():
            print("Connection state:", pc.connectionState)
            if pc.connectionState in ("failed", "closed", "disconnected"):
                await pc.close()
                pcs.discard(pc)

        # Nur Video anbieten – Audio-Transceiver ignorieren
        for transceiver in pc.getTransceivers():
            if transceiver.kind == "audio":
                # Verhindert SDP-Konflikte mit Browser
                transceiver.direction = "inactive"

        pc.addTrack(CameraStream())  # Deine Webcam

        await pc.setRemoteDescription(offer)
        answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)

        return web.Response(
            content_type="application/json",
            text=json.dumps({
                "sdp": pc.localDescription.sdp,
                "type": pc.localDescription.type
            })
        )
    except Exception as e:
        import traceback
        traceback.print_exc()
        return web.Response(status=500, text=f"Fehler: {e}")

async def handle_health(request):
    return web.Response(text="ok")

app = web.Application()
cors = aiohttp_cors.setup(app, defaults={
    "*": aiohttp_cors.ResourceOptions(
        allow_credentials=True,
        expose_headers="*",
        allow_headers="*"
    )
})

resource = app.router.add_resource("/offer")
route    = resource.add_route("POST", handle_offer)
cors.add(route)
app.router.add_get("/health", handle_health)

web.run_app(app, host="0.0.0.0", port=6060)