//
//  LicenceViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 01.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit

class LicenceViewController: PlainController, UIWebViewDelegate {
    
    @IBOutlet weak var webView: UIWebView!
   
    override func viewDidLoad() {
        super.viewDidLoad()
        
        webView.dataDetectorTypes = []
        let url = Bundle.main.url(forResource: "licenses".fileLocalized, withExtension: "html")
        let request = NSURLRequest(url: url!) as URLRequest
        webView.loadRequest(request)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        setNavBarAlpha(1.0)
    }

    func webViewDidFinishLoad(_ webView: UIWebView) {
        let css = ".link { color: \(Settings.instance.accentColor); }"
        let jsString = "var style = document.createElement('style'); style.innerHTML = '\(css)'; document.head.appendChild(style)"
        webView.stringByEvaluatingJavaScript(from: jsString)
    }
    
    func webView(_ webView: UIWebView, shouldStartLoadWith request: URLRequest, navigationType: UIWebViewNavigationType) -> Bool {
        if navigationType == UIWebViewNavigationType.linkClicked && request.url != nil {
            UIApplication.shared.openURL(request.url!)
            return false
        }
        return true
    }
  
}
