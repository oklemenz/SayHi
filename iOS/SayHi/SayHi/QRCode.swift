//
//  QRCode.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 24.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import Foundation
import AVFoundation
import CoreImage

let MaxQRCodeInputLength : Int = 2953

enum CorrectionLevel : String {
    case low = "L"
    case medium = "M"
    case quartile = "Q"
    case high = "H"
    
    static let list : [CorrectionLevel] = [.low, .medium, .quartile, .high]
}

class QRCode : NSObject, AVCaptureMetadataOutputObjectsDelegate {
    
    static let instance = QRCode()
    
    static let color = CIColor(red: 0, green: 0, blue: 0)
    static let inactiveColor = CIColor(red: 0, green: 0, blue: 0, alpha: 0.3)
    static let backgroundColor = CIColor(red: 1, green: 1, blue: 1, alpha: 0.5)
    
    func generate(text: String, size: CGFloat, correctionLevel : CorrectionLevel = .low, scale: CGFloat = 1.0, color : CIColor = color, backgroundColor : CIColor = backgroundColor) -> UIImage? {
        let data = text.data(using: .utf8)
        
        let qrFilter = CIFilter(name: "CIQRCodeGenerator")
        qrFilter!.setValue(data, forKey: "inputMessage")
        qrFilter!.setValue(correctionLevel.rawValue, forKey: "inputCorrectionLevel")
        
        let colorFilter = CIFilter(name: "CIFalseColor")
        colorFilter?.setDefaults()
        colorFilter?.setValue(qrFilter?.outputImage, forKey: "inputImage")
        colorFilter?.setValue(color, forKey: "inputColor0")
        colorFilter?.setValue(backgroundColor, forKey: "inputColor1")
        
        let qrCodeImage = colorFilter!.outputImage!
        
        let scaleX = scale * size / qrCodeImage.extent.size.width
        let scaleY = scale * size / qrCodeImage.extent.size.height
        
        let transformedImage = qrCodeImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))
        let resultImage = UIImage(ciImage: transformedImage)
        if resultImage.size.width > 0 && resultImage.size.height > 0 {
            return resultImage
        }
        return nil
    }
    
    let supportedBarCodes = [AVMetadataObject.ObjectType.qr]
    var captureSession : AVCaptureSession?
    var videoPreviewLayer : AVCaptureVideoPreviewLayer?
    var qrCodeFrameView : UIView?

    func getDefaultDevice() -> AVCaptureDevice {
        return AVCaptureDevice.default(for: AVMediaType.video)!
    }
    
    func getFrontDevice() -> AVCaptureDevice? {
        if #available(iOS 10, *) {
            return AVCaptureDevice.default(AVCaptureDevice.DeviceType.builtInWideAngleCamera, for: AVMediaType.video, position: .front)
        } else {
            for device in AVCaptureDevice.devices(for: AVMediaType.video) {
                if device.position == AVCaptureDevice.Position.front {
                    return device
                }
            }
        }
        return nil
    }
    
    func startRead(owner : UIViewController, previewView: UIView, _ completion: ((Bool) -> Void)? = nil) {
        if captureSession != nil {
            stopRead()
        }
        AVCaptureDevice.requestAccess(for: AVMediaType.video) { (allowed) in
            DispatchQueue.main.async {
                if allowed {
                    do {
                        let input = try AVCaptureDeviceInput(device: self.getFrontDevice()!)
                        self.captureSession = AVCaptureSession()
                        self.captureSession?.addInput(input)
                        let captureMetadataOutput = AVCaptureMetadataOutput()
                        self.captureSession?.addOutput(captureMetadataOutput)
                        captureMetadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
                        captureMetadataOutput.metadataObjectTypes = self.supportedBarCodes
                        
                        self.videoPreviewLayer = AVCaptureVideoPreviewLayer(session: self.captureSession!)
                        self.videoPreviewLayer?.videoGravity = AVLayerVideoGravity.resizeAspect
                        self.videoPreviewLayer?.frame = CGRect(x: 0, y: 0, width: previewView.frame.width, height: previewView.frame.height)
                        for subView in previewView.subviews {
                            subView.removeFromSuperview()
                        }
                        if let sublayers = previewView.layer.sublayers {
                            for subLayer in sublayers {
                                subLayer.removeFromSuperlayer()
                            }
                        }
                        previewView.layer.addSublayer(self.videoPreviewLayer!)
                        
                        if self.qrCodeFrameView == nil {
                            self.qrCodeFrameView = UIView()
                            self.qrCodeFrameView!.layer.borderColor = AccentColor.cgColor
                            self.qrCodeFrameView!.layer.borderWidth = 2
                        }
                        if let qrCodeFrameView = self.qrCodeFrameView {
                            previewView.addSubview(qrCodeFrameView)
                            previewView.bringSubviewToFront(qrCodeFrameView)
                        }
                        
                        self.captureSession?.startRunning()
                        if let completion = completion {
                            completion(true)
                        }
                    } catch let error {
                        print(error)
                        self.alertCameraError(error, presenter: owner)
                        if let completion = completion {
                            completion(false)
                        }
                    }
                } else {
                    self.alertCameraError(nil, presenter: owner)
                    if let completion = completion {
                        completion(false)
                    }
                }
            }
        }
    }
    
    func alertCameraError(_ error : Error?, presenter: UIViewController) {
        let errorText = error != nil ? error!.localizedDescription + "\n\n" : ""
        let alertController = UIAlertController(title: "QR Code Recognition".localized, message: errorText + "CameraError".aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        let settingsAction = UIAlertAction(title: "Settings".localized, style: .default, handler: { (action : UIAlertAction) in
            UIApplication.shared.openURL(NSURL(string:UIApplication.openSettingsURLString)! as URL)
        })
        alertController.addAction(settingsAction)
        alertController.view?.tintColor = AccentColor
        presenter.present(alertController, animated: true, completion: nil)
    }
    
    func stopRead() {
        captureSession?.stopRunning()
        captureSession = nil
        videoPreviewLayer = nil
        qrCodeFrameView = nil
    }
    
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        if metadataObjects.count == 0 {
            qrCodeFrameView?.frame = CGRect.zero
            return
        }
        
        let metadataObj = metadataObjects[0] as! AVMetadataMachineReadableCodeObject
        if supportedBarCodes.contains(metadataObj.type) {
            
            let barCodeObject = videoPreviewLayer?.transformedMetadataObject(for: metadataObj)
            qrCodeFrameView?.frame = barCodeObject!.bounds
            
            if metadataObj.stringValue != nil && !(metadataObj.stringValue?.isEmpty)! {
                NotificationCenter.default.post(name: QRCodeRecognizedNotification, object: metadataObj.stringValue)
            }
        }
    }
    
    var isTorchAvailable: Bool {
        return getDefaultDevice().isTorchAvailable
    }
    
    func switchTorchOn(mode : AVCaptureDevice.TorchMode) {
        toggleTorch(mode: .on)
    }
    
    func switchTorchOff(mode : AVCaptureDevice.TorchMode) {
        toggleTorch(mode: .off)
    }
    
    func toggleTorch(mode : AVCaptureDevice.TorchMode) {
        do {
            let defaultDevice = getDefaultDevice()
            try defaultDevice.lockForConfiguration()
            let current = defaultDevice.torchMode
            if current != mode {
                defaultDevice.torchMode = mode
                defaultDevice.unlockForConfiguration()
            }
        }
        catch let error {
            print(error)
        }
    }
}
