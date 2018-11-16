//
//  Voice.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 17.11.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation

class Voice : NSObject, AVAudioRecorderDelegate {

    static let instance = Voice()
    
    var recordingSession: AVAudioSession!
    var audioRecorder: AVAudioRecorder!
    
    struct VoiceSettings {
        var rate : Float = 0.55
        var pitch : Float = 1.0
        var volume : Float = 1.0
    }
    
    let voices : [Gender:VoiceSettings] = [
        Gender.none : VoiceSettings(rate: 0.55, pitch: 1.0, volume: 1.0),
        Gender.male : VoiceSettings(rate: 0.55, pitch: 0.75, volume: 1.0),
        Gender.female : VoiceSettings(rate: 0.55, pitch: 1.25, volume: 1.0)
    ]
    
    let speechSynthesizer = AVSpeechSynthesizer()
    var recordAlertContoller : UIAlertController?
    var recordCompletion : ((Data?) -> ())?
    var voiceFilename : URL!
    var audioPlayer : AVAudioPlayer?
    
    weak var presenter: UIViewController?
    
    override init() {
    }
    
    func speak(text: String, gender: Gender = .none) {
        let speechUtterance = AVSpeechUtterance(string: text)

        var language = Locale.current.identifier
        if !BundleLangCodes.contains(Locale.current.languageCode ?? "") {
            language = "en-US"
        }
        if let voice = AVSpeechSynthesisVoice(language: language) {
            speechUtterance.voice = voice
        }
        
        if let voice = voices[gender] {
            speechUtterance.rate = voice.rate
            speechUtterance.pitchMultiplier = voice.pitch
            speechUtterance.volume = voice.volume
        }

        speechSynthesizer.speak(speechUtterance)
    }
    
    func sayHi(presenter: UIViewController, name: String, gender: Gender = .none, toast: Bool = false) {
        var text = "Hi!".localized
        if !name.isEmpty {
            text = String(format: "Hi, I'm %@!".localized, name)
        }
        Voice.instance.speak(text: text, gender: gender)
        if toast {
            let alertController = UIAlertController(title: text, message: "DeviceNotMuted".aliasLocalized, preferredStyle: .alert)
            alertController.view?.tintColor = AccentColor
            presenter.present(alertController, animated: true, completion: nil)
            let delayTime = DispatchTime.now() + 2.0
            DispatchQueue.main.asyncAfter(deadline: delayTime) {
                alertController.dismiss(animated: true, completion: nil)
            }
        }
    }
    
    func replay(presenter : UIViewController, voice: Data) -> Bool {
        self.presenter = presenter
        audioPlayer = try? AVAudioPlayer(data: voice, fileTypeHint: "m4a")
        if let audioPlayer = audioPlayer {
            do {
                try AVAudioSession.sharedInstance().overrideOutputAudioPort(AVAudioSession.PortOverride.speaker)
                audioPlayer.volume = 1.0
                audioPlayer.prepareToPlay()
                audioPlayer.play()
                
                let alertController = UIAlertController(title: "PlayingRecording".aliasLocalized, message: "DeviceNotMuted".aliasLocalized, preferredStyle: .alert)
                alertController.view?.tintColor = AccentColor
                presenter.present(alertController, animated: true, completion: nil)
                let delayTime = DispatchTime.now() + 2.0
                DispatchQueue.main.asyncAfter(deadline: delayTime) {
                    alertController.dismiss(animated: true, completion: nil)
                }
                    
                return true
            } catch let error {
                print(error)
            }
        }
        return false
    }
    
    func record(presenter: UIViewController, message: String, completion: ((Data?) -> ())? = nil) {
        do {
            self.recordingSession = AVAudioSession.sharedInstance()
            self.recordCompletion = completion
            if #available(iOS 10.0, *) {
                try recordingSession.setCategory(.playAndRecord, mode: .default)
            } else {
                AVAudioSession.sharedInstance().perform(NSSelectorFromString("setCategory:error:"), with: AVAudioSession.Category.playback)
            }
            try recordingSession.setActive(true)
            recordingSession.requestRecordPermission() { [unowned self] allowed in
                DispatchQueue.main.async {
                    if allowed {
                        self.alertRecord(presenter: presenter, message: message)
                    } else {
                        self.alertMicrophoneError(nil, presenter: presenter)
                    }
                }
            }
        } catch let error {
            print(error)
            self.alertMicrophoneError(error, presenter: presenter)
        }
    }
    
    func alertRecord(presenter: UIViewController, message: String) {
        self.presenter = presenter
        let alertController = UIAlertController(title: "Say:".localized, message: message, preferredStyle: .alert)
        let doneAction = UIAlertAction(title: "Done".localized, style: .default, handler: { (action : UIAlertAction) in
            self.finishRecording(success: true)
        })
        alertController.addAction(doneAction)
        alertController.view?.tintColor = AccentColor
        presenter.present(alertController, animated: true, completion: nil)
        startRecording()
        recordAlertContoller = alertController
    }
    
    func startRecording() {
        voiceFilename = getDocumentsDirectory().appendingPathComponent("voice.m4a")
        
        let settings = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 12000,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue
        ]
        
        do {
            audioRecorder = try AVAudioRecorder(url: voiceFilename, settings: settings)
            audioRecorder.delegate = self
            audioRecorder.record(forDuration: 3.0)
        } catch let error {
            print(error)
            finishRecording(success: false)
        }
    }
    
    func finishRecording(success: Bool) {
        if audioRecorder == nil {
            return
        }
        
        audioRecorder.stop()
        audioRecorder = nil
        
        if let recordAlertContoller = recordAlertContoller {
            recordAlertContoller.dismiss(animated: true, completion: nil)
        }
        recordAlertContoller = nil

        var voiceData : Data?
        if success {
            voiceData = try? Data(contentsOf: voiceFilename!)
        }
        
        try? FileManager.default.removeItem(at: voiceFilename)
        if let recordCompletion = recordCompletion {
            recordCompletion(voiceData)
            if let voiceData = voiceData {
                if let presenter = self.presenter {
                    _ = self.replay(presenter: presenter, voice: voiceData)
                }
            }
        }
        recordCompletion = nil
        presenter = nil
    }
    
    func audioRecorderDidFinishRecording(_ recorder: AVAudioRecorder, successfully flag: Bool) {
        finishRecording(success: flag)
    }

    func alertMicrophoneError(_ error : Error?, presenter: UIViewController) {
        let errorText = error != nil ? error!.localizedDescription + "\n\n" : ""
        let alertController = UIAlertController(title: "Voice Recording".localized, message: errorText + "MicrophoneError".aliasLocalized, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
        alertController.addAction(okAction)
        let settingsAction = UIAlertAction(title: "Settings".localized, style: .default, handler: { (action : UIAlertAction) in
            UIApplication.shared.openURL(NSURL(string:UIApplication.openSettingsURLString)! as URL)
        })
        alertController.addAction(settingsAction)
        alertController.view?.tintColor = AccentColor
        presenter.present(alertController, animated: true, completion: nil)
    }
    
    func getDocumentsDirectory() -> URL {
        let paths = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)
        let documentsDirectory = paths[0]
        return documentsDirectory
    }
}
