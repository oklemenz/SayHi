//
//  LocationViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 10.02.17.
//  Copyright © 2017 Klemenz, Oliver. All rights reserved.
//

import Foundation
import MapKit
import CoreLocation

class LocationViewController: PlainController {
    
    @IBOutlet weak var mapView: MKMapView!
    @IBOutlet weak var ribbonView: UIView!
    
    @IBAction func setViewport(_ sender: Any) {
        displayViewport()
    }
    
    func displayViewport() {
        if let location = location {
            let center = CLLocationCoordinate2D(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude)
            let region = MKCoordinateRegion(
                center: center,
                span: MKCoordinateSpan(latitudeDelta: 0.05, longitudeDelta: 0.05))
            self.mapView.setRegion(region, animated: true)
        }
    }
    
    var _location: CLLocation?
    var location: CLLocation? {
        set {
            _ = view
            _location = newValue
            displayViewport()
        }
        get {
            return _location
        }
    }
    
    func setLocationDescription(name: String, street: String, city: String, country: String) {
        let text = NSMutableAttributedString(
            string: name + "\n",
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor])
        
        var separator = ""
        if !street.isEmpty {
            text.append(NSMutableAttributedString(
                string: separator + street,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            separator = ", "
        }
        if !city.isEmpty {
            text.append(NSMutableAttributedString(
                string: separator + city,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            separator = ", "
        }
        if !country.isEmpty {
            text.append(NSMutableAttributedString(
                string: separator + country,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            separator = ", "
        }
        let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
        label.backgroundColor = UIColor.clear
        label.numberOfLines = 2
        label.font = UIFont.boldSystemFont(ofSize: 16.0)
        label.textAlignment = .center
        label.textColor = UIColor.white
        label.attributedText = text
        self.navigationItem.titleView = label
        
        if let location = location {
            let annotation = MKPointAnnotation()
            annotation.coordinate = location.coordinate
            annotation.title = name
            mapView.addAnnotation(annotation)
            mapView.selectAnnotation(annotation, animated: true)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        setNavBarAlpha(1.0)

        let backgroundLayer = BackgroundLayer()
        backgroundLayer.frame = self.ribbonView.bounds
        self.ribbonView.layer.insertSublayer(backgroundLayer, at: 0)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        setNavBarAlpha(0.0)
    }
}
