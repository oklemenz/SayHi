//
//  MatchViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 09.12.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import Foundation
import UIKit
import CoreLocation

class MatchViewController: PlainController, UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout {
    
    @IBOutlet weak var infoButton: UIBarButtonItem!
    
    @IBOutlet weak var topContainerView: UIView!
    @IBOutlet weak var bothPosTagCollectionView: UICollectionView!
    @IBOutlet weak var bothNegTagCollectionView: UICollectionView!
    @IBOutlet weak var onlyPosTagCollectionView: UICollectionView!
    @IBOutlet weak var onlyNegTagCollectionView: UICollectionView!
    
    @IBOutlet weak var bothPosTagLabel: UILabel!
    @IBOutlet weak var bothNegTagLabel: UILabel!
    @IBOutlet weak var onlyPosTagLabel: UILabel!
    @IBOutlet weak var onlyNegTagLabel: UILabel!
    @IBOutlet weak var separatorLine: UIView!
    
    @IBOutlet weak var navigationBarBackground: UIVisualEffectView!
    @IBOutlet weak var separatorBarBackground: UIVisualEffectView!
    
    @IBOutlet weak var headerViewHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var bottomViewHeightConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var topLeftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var topLeftLabelWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var topRightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var topRightLabelWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var topSpaceConstraint: NSLayoutConstraint!
    @IBOutlet weak var bottomHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var leftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var middleHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpBottomHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpLeftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpRightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var statusContainerView: UIView!
    @IBOutlet weak var statusLabel: UILabel!
    
    @IBOutlet weak var locationButton: UIButton!
    
    @IBOutlet weak var helpLeftContainerView: UIView!
    @IBOutlet weak var helpRightContainerView: UIView!
    @IBOutlet weak var helpStatusArrow: UIImageView!
    @IBOutlet weak var helpStatusLabel: UILabel!
    @IBOutlet weak var helpLocationArrow: UIImageView!
    @IBOutlet weak var helpLocationLabel: UILabel!
    
    @IBOutlet weak var helpShowsTagsBothLikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsBothLikeLabel: UILabel!
    @IBOutlet weak var helpShowsTagsBothDislikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsBothDislikeLabel: UILabel!
    @IBOutlet weak var helpShowsTagsLikeDislikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsLikeDislikeLabel: UILabel!
    @IBOutlet weak var helpShowsTagsDislikeLikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsDislikeLikeLabel: UILabel!

    @IBOutlet weak var helpTopLeftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpTopRightWidthConstraint: NSLayoutConstraint!
    
    @IBOutlet var helpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        self.helpView.show(owner: self)
    }
    
    @IBAction func infoPressed(_ sender: Any) {
        let alertController = UIAlertController(
            title: "MatchingDetails".aliasLocalized,
            message: String(format: "MatchingDifferentLanguage".aliasLocalized,
                            Locale.current.localizedString(forIdentifier: match.matchLangCode) ?? "",
                            Locale.current.localizedString(forIdentifier: match.langCode) ?? ""),
            preferredStyle: .alert)
        
        let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: { (action : UIAlertAction) in
        })
        alertController.addAction(okAction)
        
        alertController.view?.tintColor = AccentColor
        self.activeViewController.present(alertController, animated: true, completion: nil)
    }
    
    var bottomBarVisible: Bool = true
    var tagSizingCell: TagCell?
    
    var _match : Match!
    var match : Match {
        set {
            _match = newValue
            
            let text = NSMutableAttributedString(
                string: !match.firstName.isEmpty ? match.firstName : "?".localized,
                attributes: [NSAttributedString.Key.foregroundColor: AccentColor])
            if match.gender != .none {
                text.append(NSMutableAttributedString(
                    string: ", " +
                        String(format: "\(match.gender.rawValue)_short".codeLocalized),
                    attributes: [NSAttributedString.Key.foregroundColor: AccentColor]))
            }
            if match.birthYear >= BaseYear {
                text.append(NSMutableAttributedString(
                    string: ", " +
                        String(format: "~%i y.".localized, match.age),
                    attributes: [NSAttributedString.Key.foregroundColor: AccentColor]))
            }
            text.append(NSMutableAttributedString(
                string: ", \(match.langCode.uppercased())",
                attributes: [NSAttributedString.Key.foregroundColor: AccentColor]))
            text.append(NSMutableAttributedString(
                string: "\n" +
                    match.profileName,
                attributes: [NSAttributedString.Key.foregroundColor: UIColor.black,
                             NSAttributedString.Key.font: UIFont.systemFont(ofSize: 12.0)]))                             
            
            if match.relationType != .none {
                text.append(NSMutableAttributedString(
                    string: SeparatorString + Emoji.relationType + match.relationType.rawValue.codeLocalized,
                    attributes: [NSAttributedString.Key.foregroundColor: UIColor.black,
                                 NSAttributedString.Key.font: UIFont.systemFont(ofSize: 12.0)]))
            }
            text.append(NSMutableAttributedString(
                string: SeparatorString + Emoji.matchMode + match.mode.description.codeLocalized,
                attributes: [NSAttributedString.Key.foregroundColor: UIColor.black,
                             NSAttributedString.Key.font: UIFont.systemFont(ofSize: 12.0)]))
            
            let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
            label.backgroundColor = UIColor.clear
            label.numberOfLines = 2
            label.font = UIFont.boldSystemFont(ofSize: 16.0)
            label.textAlignment = .center
            label.textColor = UIColor.white
            label.attributedText = text
            self.navigationItem.titleView = label

            _ = view

            if !match.status.isEmpty {
                statusLabel.text = String(format: "»%@«".localized, match.status)
            } else {
                helpStatusArrow.isHidden = true
                helpStatusLabel.isHidden = true
            }
            
            if !match.locationCity.isEmpty {
                locationButton.setTitle(match.locationCity, for: .normal)
            }
            if match.locationLatitude.isEmpty || match.locationLongitude.isEmpty {
                locationButton.isHidden = true
                helpLocationArrow.isHidden = true
                helpLocationLabel.isHidden = true
            }
           
            self.bottomBarVisible =
                !match.status.isEmpty ||
                (!match.locationLatitude.isEmpty && !match.locationLongitude.isEmpty)

            if !self.bottomBarVisible {
                if match.mode != .exact {
                    bottomHeightConstraint.constant = -22.0
                }
                onlyPosTagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight, left: 0, bottom: 0, right: 0)
                onlyNegTagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight, left: 0, bottom: 0, right: 0)
                statusContainerView.isHidden = true
            }
            
            helpShowsTagsBothLikeLabel.text = "ShowsTagsBothLike".termLocalized(Emoji.like)
            if helpShowsTagsBothLikeLabel.text!.isEmpty {
               helpShowsTagsBothLikeArrow.isHidden = true
            }
            helpShowsTagsBothDislikeLabel.text = "ShowsTagsBothDislike".termLocalized(Emoji.dislike)
            if helpShowsTagsBothDislikeLabel.text!.isEmpty {
                helpShowsTagsBothDislikeArrow.isHidden = true
            }
            helpShowsTagsLikeDislikeLabel.text = "ShowsTagsLikeDislike".termLocalized(Emoji.like, Emoji.dislike)
            if helpShowsTagsLikeDislikeLabel.text!.isEmpty {
                helpShowsTagsLikeDislikeArrow.isHidden = true
            }
            helpShowsTagsDislikeLikeLabel.text = "ShowsTagsDislikeLike".termLocalized(Emoji.like)
            if helpShowsTagsDislikeLikeLabel.text!.isEmpty {
                helpShowsTagsDislikeLikeArrow.isHidden = true
            }
            
            if match.matchLangCode.isEmpty || match.matchLangCode == match.langCode {
                if let rightBarButtonItems = self.navigationItem.rightBarButtonItems {
                    if let index = rightBarButtonItems.firstIndex(of: infoButton) {
                        self.navigationItem.rightBarButtonItems?.remove(at: index)
                    }
                }
            }
            
            updateLabels()
            updateCollectionViewLayout()
        }
        get { return _match }
    }
    var profile : Profile!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.automaticallyAdjustsScrollViewInsets = false
        
        self.navigationBarBackground.alpha = 0
        self.separatorBarBackground.alpha = 0
        
        var edgeInsetTop = NavBarHeight + InfoBarHeight
        if #available(iOS 11.0, *) {
            edgeInsetTop = InfoBarHeight
        }
        
        if isIPhoneX() {
            headerViewHeightConstraint.constant = StatusBarHeight + NavBarHeight + InfoBarHeight
            bottomViewHeightConstraint.constant = BottomBarHeightX
        }
        
        let tagCellNib = UINib(nibName: "TagCell", bundle:nil)
        self.bothPosTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.bothPosTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight, right: 0)
        self.bothPosTagCollectionView.delegate = self
        self.bothPosTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.bothNegTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.bothNegTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight, right: 0)
        self.bothNegTagCollectionView.delegate = self
        self.bothNegTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.onlyPosTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.onlyPosTagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight, left: 0, bottom: BottomBarHeight, right: 0)
        self.onlyPosTagCollectionView.delegate = self
        self.onlyPosTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.onlyNegTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.onlyNegTagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight, left: 0, bottom: BottomBarHeight, right: 0)
        self.onlyNegTagCollectionView.delegate = self
        self.onlyNegTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.tagSizingCell = (tagCellNib.instantiate(withOwner: nil, options: nil) as NSArray).firstObject as! TagCell?
    }
    
    deinit {
        self.bothPosTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        self.bothNegTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        self.onlyPosTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        self.onlyNegTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
    }

    func updateCollectionViewLayout() {
        _ = self.view

        var edgeInsetTop = NavBarHeight + InfoBarHeight
        if #available(iOS 11.0, *) {
            edgeInsetTop = InfoBarHeight
        }
        
        switch match.mode {
            case .basic:
                fallthrough
            case .exact:
                topSpaceConstraint.constant = 0
                bottomHeightConstraint = bottomHeightConstraint.setMultiplier(multiplier: 0.0)
                helpBottomHeightConstraint = helpBottomHeightConstraint.setMultiplier(multiplier: 0.0)
                if self.bottomBarVisible {
                    bothPosTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: BottomBarHeight, right: 0)
                    bothNegTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: BottomBarHeight, right: 0)
                } else {
                    bothPosTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: 0, right: 0)
                    bothNegTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: 0, right: 0)
                    middleHeightConstraint.constant = 0
                }
                helpRightContainerView.isHidden = true
                helpLeftContainerView.isHidden = true
                if match.mode == .basic {
                    topLeftWidthConstraint = topLeftWidthConstraint.setMultiplier(multiplier: 1.0)
                    topLeftLabelWidthConstraint = topLeftLabelWidthConstraint.setMultiplier(multiplier: 1.0)
                    topRightWidthConstraint = topRightWidthConstraint.setConstant(constant: 0.0)
                    topRightLabelWidthConstraint = topRightLabelWidthConstraint.setConstant(constant: 0.0)
                    separatorLine.isHidden = true
                    helpTopLeftWidthConstraint = helpTopLeftWidthConstraint.setMultiplier(multiplier: 1.0)
                    helpTopRightWidthConstraint = helpTopRightWidthConstraint.setConstant(constant: 0.0)
                }
            case .adapt:
                leftWidthConstraint = leftWidthConstraint.setMultiplier(multiplier: 1.0)
                rightWidthConstraint = rightWidthConstraint.setMultiplier(multiplier: 0.0)
                middleHeightConstraint = middleHeightConstraint.setFirstItem(topContainerView)
                middleHeightConstraint.constant = InfoBarHeight
                helpLeftWidthConstraint = helpLeftWidthConstraint.setMultiplier(multiplier: 1.0)
                helpRightWidthConstraint = helpRightWidthConstraint.setMultiplier(multiplier: 0.0)
                helpRightContainerView.isHidden = true
            case .tries:
                leftWidthConstraint = leftWidthConstraint.setMultiplier(multiplier: 0.0)
                rightWidthConstraint = rightWidthConstraint.setMultiplier(multiplier: 1.0)
                middleHeightConstraint = middleHeightConstraint.setFirstItem(topContainerView)
                middleHeightConstraint.constant = InfoBarHeight
                helpLeftWidthConstraint = helpLeftWidthConstraint.setMultiplier(multiplier: 0.0)
                helpRightWidthConstraint = helpRightWidthConstraint.setMultiplier(multiplier: 1.0)
                helpLeftContainerView.isHidden = true
            case .open:
                if !self.bottomBarVisible {
                    middleHeightConstraint.constant = 0
                }
        }

        self.view.layoutIfNeeded()
    }
    
    func tagsForCollectionView(_ collectionView: UICollectionView) -> [Tag] {
        switch collectionView {
        case bothPosTagCollectionView!:
            return match.bothPosTags
        case bothNegTagCollectionView!:
            return match.bothNegTags
        case onlyPosTagCollectionView!:
            return match.onlyPosTags
        case onlyNegTagCollectionView:
            return match.onlyNegTags
        default:
            return []
        }
    }
    
    // MARK: UICollectionViewDataSource
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return tagsForCollectionView(collectionView).count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let tagCell = collectionView.dequeueReusableCell(withReuseIdentifier: "TagCell", for: indexPath) as! TagCell
        let tag = tagsForCollectionView(collectionView)[indexPath.item]
        tagCell.data = tag
        tagCell.isHidden = false
        if let collectionView = collectionView as? DragAndDropCollectionView {
            if let draggingPathOfCellBeingDragged = collectionView.draggingPathOfCellBeingDragged {
                if draggingPathOfCellBeingDragged.item == indexPath.item {
                    tagCell.isHidden = true
                }
            }
        }
        return tagCell
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        var maxWidth = TagMaxWidth
        if (match.mode != .basic && (collectionView == bothPosTagCollectionView! || collectionView == bothNegTagCollectionView!)) || match.mode == .open {
            maxWidth = TagMaxHalfWidth
        }
        let tag = tagsForCollectionView(collectionView)[indexPath.item]
        self.tagSizingCell?.tagView.button.setTitle(tag.name + IconPlaceholder, for: .normal)
        return CGSize(width: min(self.tagSizingCell!.tagView.systemLayoutSizeFitting(UIView.layoutFittingCompressedSize).width, maxWidth), height: ContentSize)
    }

    func updateLabels() {
        bothPosTagLabel.text = String(format: "BothLikeNum".localized, Emoji.like, Emoji.like, match.bothPosTags.count, match.profilePosTagCount, match.messagePosTagCount)
        bothNegTagLabel.text = String(format: "BothDislikeNum".localized, Emoji.dislike, Emoji.dislike, match.bothNegTags.count, match.profileNegTagCount, match.messageNegTagCount)
        onlyPosTagLabel.text = String(format: "OnlyLikeNum".localized, Emoji.like, Emoji.dislike, match.onlyPosTags.count, match.profilePosTagCount, match.messagePosTagCount)
        onlyNegTagLabel.text = String(format: "OnlyDislikeNum".localized, Emoji.dislike, Emoji.like, match.onlyNegTags.count, match.profileNegTagCount, match.messageNegTagCount)
    }
    
    // MARK: UIScrollViewDelegate
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        DispatchQueue.main.async() {
            self.setScrollAlpha(scrollView)
        }
    }
    
    func setScrollAlpha(_ scrollView : UIScrollView? = nil) {
        if let scrollView = scrollView {
            if scrollView == bothPosTagCollectionView || scrollView == bothNegTagCollectionView {
                var contentOffsetY =
                    max(bothPosTagCollectionView.contentInset.top + bothPosTagCollectionView.contentOffset.y,
                        bothNegTagCollectionView.contentInset.top + bothNegTagCollectionView.contentOffset.y)
                if #available(iOS 11.0, *) {
                    contentOffsetY = contentOffsetY + NavBarHeight
                }
                var alpha = contentOffsetY / BlurAlphaRatio
                if alpha < 0 {
                    alpha = 0
                }
                if alpha > 1 {
                    alpha = 1
                }
                navigationBarBackground.alpha = alpha
            }
        }
        let contentOffsetYPos = bothPosTagCollectionView.contentSize.height + bothPosTagCollectionView.contentInset.bottom - bothPosTagCollectionView.contentOffset.y - bothPosTagCollectionView.frame.size.height
        let contentOffsetYNeg = bothNegTagCollectionView.contentSize.height + bothNegTagCollectionView.contentInset.bottom - bothNegTagCollectionView.contentOffset.y - bothNegTagCollectionView.frame.size.height
        let contentOffsetY =
            max(max(onlyPosTagCollectionView.contentInset.top + onlyPosTagCollectionView.contentOffset.y,
                    onlyNegTagCollectionView.contentInset.top + onlyNegTagCollectionView.contentOffset.y),
                max(contentOffsetYPos, contentOffsetYNeg))
        var alpha = contentOffsetY / BlurAlphaRatio
        if alpha < 0 {
            alpha = 0
        }
        if alpha > 1 {
            alpha = 1
        }
        separatorBarBackground.alpha = alpha
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        setScrollAlpha()
    }
    
    // MARK: Segue
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "profile",
            let tagViewController = segue.destination as? TagViewController {
            tagViewController.profile = profile
            tagViewController.makeReadOnly()
        } else if segue.identifier == "location",
            let locationViewController = segue.destination as? LocationViewController {
            if let latitude = Double(match.locationLatitude),
               let longitude = Double(match.locationLongitude) {
                locationViewController.location = CLLocation(latitude: latitude, longitude: longitude)
                locationViewController.setLocationDescription(name: match.locationName,
                                                              street: match.locationStreet,
                                                              city: match.locationCity,
                                                              country: match.locationCountry)
            }
        }
    }
}
