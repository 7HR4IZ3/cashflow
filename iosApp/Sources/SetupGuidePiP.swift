import AVFoundation
import AVKit
import UIKit

/// A small floating card that stays on screen when the person leaves Cashflow for Shortcuts.
///
/// iOS does not let an app draw over other apps. Picture in Picture is the one floating window it does allow,
/// so the current setup step (with the real screenshot) is rendered into a tiny "video" and shown there.
/// It follows the person into Shortcuts and goes away on its own when they come back.
final class SetupGuidePiP: NSObject, AVPictureInPictureControllerDelegate, AVPictureInPictureSampleBufferPlaybackDelegate {
    static let shared = SetupGuidePiP()

    private let displayLayer = AVSampleBufferDisplayLayer()
    private var pip: AVPictureInPictureController?
    private var host: UIView?
    private var timer: Timer?
    // The loop: each step is lit for `dwell` seconds, then the highlight slides on over `glide` seconds.
    private var epoch = Date()
    private let dwell = 2.6
    private let glide = 0.55
    private var startedAt = Date.distantPast
    // Only true while the setup guide is on screen. Anywhere else the card must never appear.
    private var allowed = false
    // Portrait, so iOS opens a tall window that holds the whole setup at once.
    private let size = CGSize(width: 540, height: 960)

    private override init() {
        super.init()
        NotificationCenter.default.addObserver(self, selector: #selector(appBecameActive), name: UIApplication.didBecomeActiveNotification, object: nil)
    }

    // MARK: Public

    /// The setup guide appeared (true) or went away (false). Going away also removes any card that is showing.
    func setAllowed(_ value: Bool) {
        DispatchQueue.main.async {
            self.allowed = value
            if !value { self.stop() }
        }
    }

    /// Show the card for [step] (1...3). Step 0 hides it.
    func start(step: Int) {
        DispatchQueue.main.async {
            guard step > 0 else { self.stop(); return }
            guard self.allowed else { return }
            guard AVPictureInPictureController.isPictureInPictureSupported() else { return }
            // Start the loop on the requested step.
            self.epoch = Date().addingTimeInterval(-Double(min(max(step, 1), 3) - 1) * (self.dwell + self.glide))
            self.prepare()
            self.render()
            self.startedAt = Date()
            self.tryStart(attempt: 0)
        }
    }

    func stop() {
        timer?.invalidate(); timer = nil
        pip?.stopPictureInPicture()
        displayLayer.flushAndRemoveImage()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    /// Runs [action] once the floating card is up (or after a short wait), so the card is on screen before the app leaves.
    func whenReady(_ action: @escaping () -> Void) {
        guard pip != nil, startedAt.timeIntervalSinceNow > -3 else { action(); return }
        func poll(_ n: Int) {
            if self.pip?.isPictureInPictureActive == true || n > 15 { action(); return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { poll(n + 1) }
        }
        poll(0)
    }

    // MARK: Setup

    private func prepare() {
        if pip == nil {
            let session = AVAudioSession.sharedInstance()
            // Mix with other audio so the person's music is never interrupted.
            try? session.setCategory(.playback, mode: .moviePlayback, options: [.mixWithOthers])
            try? session.setActive(true)

            displayLayer.videoGravity = .resizeAspect
            displayLayer.frame = CGRect(x: 0, y: 0, width: 36, height: 64)
            var timebase: CMTimebase?
            CMTimebaseCreateWithSourceClock(allocator: kCFAllocatorDefault, sourceClock: CMClockGetHostTimeClock(), timebaseOut: &timebase)
            if let timebase {
                CMTimebaseSetTime(timebase, time: CMClockGetTime(CMClockGetHostTimeClock()))
                CMTimebaseSetRate(timebase, rate: 1)
                displayLayer.controlTimebase = timebase
            }
            // The layer must sit in the visible view tree for PiP to accept it; the app's own view covers it.
            if host == nil,
               let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController?.view {
                let v = UIView(frame: CGRect(x: 0, y: 0, width: 36, height: 64))
                v.layer.addSublayer(displayLayer)
                root.insertSubview(v, at: 0)
                host = v
            }
            let source = AVPictureInPictureController.ContentSource(sampleBufferDisplayLayer: displayLayer, playbackDelegate: self)
            let controller = AVPictureInPictureController(contentSource: source)
            controller.delegate = self
            controller.canStartPictureInPictureAutomaticallyFromInline = true
            pip = controller
        }
        if timer == nil {
            // Keep a fresh frame flowing; a still picture is still "video" as far as PiP cares.
            timer = Timer.scheduledTimer(withTimeInterval: 1.0 / 12.0, repeats: true) { [weak self] _ in self?.render() }
        }
    }

    private func tryStart(attempt: Int) {
        guard let pip else { return }
        if pip.isPictureInPictureActive { return }
        if pip.isPictureInPicturePossible { pip.startPictureInPicture(); return }
        if attempt < 30 { DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { self.tryStart(attempt: attempt + 1) } }
    }

    @objc private func appBecameActive() {
        // Opening a link bounces the app through "active"; only a real return should dismiss the card.
        if Date().timeIntervalSince(startedAt) > 2.5, pip?.isPictureInPictureActive == true { stop() }
    }

    // MARK: Frames

    private func render() {
        guard let image = card(focus: focus()), let buffer = pixelBuffer(from: image) else { return }
        var format: CMVideoFormatDescription?
        CMVideoFormatDescriptionCreateForImageBuffer(allocator: nil, imageBuffer: buffer, formatDescriptionOut: &format)
        guard let format else { return }
        var timing = CMSampleTimingInfo(duration: .invalid, presentationTimeStamp: CMClockGetTime(CMClockGetHostTimeClock()), decodeTimeStamp: .invalid)
        var sample: CMSampleBuffer?
        CMSampleBufferCreateReadyWithImageBuffer(allocator: nil, imageBuffer: buffer, formatDescription: format, sampleTiming: &timing, sampleBufferOut: &sample)
        guard let sample else { return }
        if let attachments = CMSampleBufferGetSampleAttachmentsArray(sample, createIfNecessary: true) as? [NSMutableDictionary], let first = attachments.first {
            first[kCMSampleAttachmentKey_DisplayImmediately] = true
        }
        if displayLayer.status == .failed { displayLayer.flush() }
        displayLayer.enqueue(sample)
    }

    private let titles = ["Add the shortcut", "Pick your banks", "Run quietly"]
    private let images = ["setup_tile", "setup_pick_banks", "setup_quiet"]
    private var shots: [String: UIImage] = [:]

    private func screenshot(named name: String) -> UIImage? {
        if let cached = shots[name] { return cached }
        let path = "compose-resources/composeResources/cashflow.composeapp.generated.resources/drawable/\(name).png"
        let image = UIImage(contentsOfFile: Bundle.main.bundleURL.appendingPathComponent(path).path)
        if let image { shots[name] = image }
        return image
    }

    /// Where the highlight is, as a position between 0 and 3 that wraps (2.5 sits between step 3 and step 1).
    private func focus() -> CGFloat {
        let slot = dwell + glide
        let t = max(0, Date().timeIntervalSince(epoch)).truncatingRemainder(dividingBy: slot * 3)
        let index = floor(t / slot)
        let within = t - index * slot
        guard within > dwell else { return CGFloat(index) }
        let u = (within - dwell) / glide
        let eased = u * u * (3 - 2 * u)
        return CGFloat(index) + CGFloat(eased)
    }

    /// 1 when the highlight is on [index], fading to 0 as it moves away (the loop wraps around).
    private func weight(_ index: Int, _ focus: CGFloat) -> CGFloat {
        let raw = abs(focus - CGFloat(index))
        let d = min(raw, 3 - raw)
        return max(0, 1 - d)
    }

    /// All three steps, top to bottom, with a spotlight that glides over whichever one is current.
    private func card(focus f: CGFloat) -> UIImage? {
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        let citron = UIColor(red: 0.882, green: 0.961, blue: 0.549, alpha: 1)
        let ink = UIColor(red: 0.961, green: 0.957, blue: 0.933, alpha: 1)
        let dark = UIColor(red: 0.063, green: 0.071, blue: 0.075, alpha: 1)
        let centered = NSMutableParagraphStyle()
        centered.alignment = .center

        // Top of each section (a little lower than the edge so the system's close button never covers a title).
        let tops: [CGFloat] = [56, 244, 732]
        let maxHeights: [CGFloat] = [96, 400, 130]

        return UIGraphicsImageRenderer(size: size, format: format).image { ctx in
            dark.setFill()
            UIRectFill(CGRect(origin: .zero, size: size))

            for index in 0..<3 {
                let w = weight(index, f)
                let top = tops[index]
                let fade = 0.38 + 0.62 * w

                var picture = CGRect.zero
                var shot: UIImage?
                if let image = screenshot(named: images[index]) {
                    let maxWidth: CGFloat = size.width - 56
                    let scale = min(maxWidth / image.size.width, maxHeights[index] / image.size.height)
                    picture = CGRect(x: 28, y: top + 66, width: image.size.width * scale, height: image.size.height * scale)
                    shot = image
                }

                // Spotlight panel behind the active step.
                if w > 0.02 {
                    let panel = CGRect(x: 12, y: top - 16, width: size.width - 24, height: (picture.maxY - top) + 32)
                    citron.withAlphaComponent(0.09 * w).setFill()
                    let path = UIBezierPath(roundedRect: panel, cornerRadius: 30)
                    path.fill()
                    citron.withAlphaComponent(0.55 * w).setStroke()
                    path.lineWidth = 3
                    path.stroke()
                }

                // Badge: fills with citron as the highlight arrives.
                let badge = CGRect(x: 28, y: top, width: 50, height: 50)
                citron.withAlphaComponent(0.25 + 0.75 * w).setFill()
                if w > 0.5 {
                    UIBezierPath(ovalIn: badge).fill()
                } else {
                    UIColor(white: 1, alpha: 0.28).setStroke()
                    let ring = UIBezierPath(ovalIn: badge.insetBy(dx: 1.5, dy: 1.5))
                    ring.lineWidth = 3
                    ring.stroke()
                }
                ("\(index + 1)" as NSString).draw(in: CGRect(x: badge.minX, y: badge.minY + 7, width: badge.width, height: 38), withAttributes: [
                    .font: UIFont.boldSystemFont(ofSize: 30),
                    .foregroundColor: w > 0.5 ? dark : ink.withAlphaComponent(0.6),
                    .paragraphStyle: centered,
                ])
                (titles[index] as NSString).draw(at: CGPoint(x: 94, y: top + 6), withAttributes: [
                    .font: UIFont.boldSystemFont(ofSize: 36), .foregroundColor: ink.withAlphaComponent(fade),
                ])

                if let shot {
                    ctx.cgContext.saveGState()
                    UIBezierPath(roundedRect: picture, cornerRadius: 18).addClip()
                    shot.draw(in: picture, blendMode: .normal, alpha: fade)
                    ctx.cgContext.restoreGState()
                }
            }
        }
    }

    private func pixelBuffer(from image: UIImage) -> CVPixelBuffer? {
        guard let cg = image.cgImage else { return nil }
        let attrs = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true,
            kCVPixelBufferIOSurfacePropertiesKey: [String: Any](),
        ] as CFDictionary
        var buffer: CVPixelBuffer?
        CVPixelBufferCreate(kCFAllocatorDefault, Int(size.width), Int(size.height), kCVPixelFormatType_32BGRA, attrs, &buffer)
        guard let buffer else { return nil }
        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }
        guard let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer), width: Int(size.width), height: Int(size.height), bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer), space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        ) else { return nil }
        context.draw(cg, in: CGRect(origin: .zero, size: size))
        return buffer
    }

    // MARK: AVPictureInPictureSampleBufferPlaybackDelegate

    func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, setPlaying playing: Bool) { }

    func pictureInPictureControllerTimeRangeForPlayback(_ pictureInPictureController: AVPictureInPictureController) -> CMTimeRange {
        CMTimeRange(start: .negativeInfinity, duration: .positiveInfinity)
    }

    func pictureInPictureControllerIsPlaybackPaused(_ pictureInPictureController: AVPictureInPictureController) -> Bool { false }

    func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, didTransitionToRenderSize newRenderSize: CMVideoDimensions) { }

    func pictureInPictureController(_ pictureInPictureController: AVPictureInPictureController, skipByInterval skipInterval: CMTime, completion completionHandler: @escaping () -> Void) {
        // The skip buttons jump the highlight to the next or previous step and keep the loop running from there.
        let slot = dwell + glide
        let current = Int((focus() + 0.5).rounded(.down)) % 3
        let target = skipInterval.seconds >= 0 ? (current + 1) % 3 : (current + 2) % 3
        epoch = Date().addingTimeInterval(-Double(target) * slot)
        render()
        completionHandler()
    }
}
