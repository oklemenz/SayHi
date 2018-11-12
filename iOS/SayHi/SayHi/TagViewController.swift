//
//  ViewController.swift
//  SayHi
//
//  Created by Klemenz, Oliver on 12.10.16.
//  Copyright © 2016 Klemenz, Oliver. All rights reserved.
//

import UIKit
import MessageUI
import PromiseKit

let IconPlaceholder: String = "       "

let ScrollThreshold: CGFloat = 50
let InfoBarHeight: CGFloat = 30
let BottomBarHeight: CGFloat = 44
let StatusBarDelta: CGFloat = 24
var BottomSafeInsetX: CGFloat = 0
var BottomBarHeightX: CGFloat = BottomBarHeight + BottomSafeInsetX
let BarSpace: CGFloat = 8
let ContentSize: CGFloat = 25
let MaxTagTransparent: CGFloat = 0.3

var NoTagAlertShown: Bool = false

class TagViewController: PlainController, DragAndDropCollectionViewDataSource, NewTagTableControllerDelegate, UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout, DragAndDropManagerDelegate, CategoryCellDelegate, UISearchBarDelegate, SearchCategoryViewControllerDelegate {
    
    @IBOutlet weak var tagContainerView: UIView!
    @IBOutlet weak var searchContainerView: UIView!
    
    @IBOutlet weak var posTagCollectionView: DragAndDropCollectionView!
    @IBOutlet weak var negTagCollectionView: DragAndDropCollectionView!
    @IBOutlet weak var tagCollectionView: DragAndDropCollectionView!
    @IBOutlet weak var categoryCollectionView: UICollectionView!
    
    @IBOutlet weak var posTagLabel: UILabel!
    @IBOutlet weak var negTagLabel: UILabel!
    @IBOutlet weak var tagLabel: UILabel!
    @IBOutlet weak var separatorLine: UIView!
    
    @IBOutlet weak var navigationBarBackground: UIVisualEffectView!
    @IBOutlet weak var searchBarBackground: UIVisualEffectView!
    @IBOutlet weak var toolBarBackground: UIVisualEffectView!
    
    @IBOutlet weak var searchBar: UISearchBar!
    
    @IBOutlet weak var headerViewHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var bottomViewHeightConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var leftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var leftLabelWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightLabelWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var viewBottomConstraint: NSLayoutConstraint!
    @IBOutlet weak var searchContainerConstraint: NSLayoutConstraint!
    @IBOutlet weak var middleHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var bottomHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var topSpaceConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var categoryActivityIndicator: UIActivityIndicatorView!
    @IBOutlet weak var tagActivityIndicator: UIActivityIndicatorView!
    
    @IBOutlet weak var newTagButton: UIBarButtonItem!
    @IBOutlet weak var matchButton: UIBarButtonItem!
    
    @IBOutlet weak var helpStatusArrow: UIImageView!
    @IBOutlet weak var helpStatusLabel: UILabel!
    @IBOutlet weak var helpNewTagLabel: UILabel!
    @IBOutlet weak var helpNewTagArrow: UIImageView!
    
    @IBOutlet weak var helpDropTagsTopicsLikeArrow: UIImageView!
    @IBOutlet weak var helpDropTagsTopicsLikeLabel: UILabel!
    @IBOutlet weak var helpDropTagsTopicsDislikeArrow: UIImageView!
    @IBOutlet weak var helpDropTagsTopicsDislikeLabel: UILabel!
    @IBOutlet weak var helpShowsTagsLikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsLikeLabel: UILabel!
    @IBOutlet weak var helpShowsTagsDislikeArrow: UIImageView!
    @IBOutlet weak var helpShowsTagsDislikeLabel: UILabel!
    
    @IBOutlet weak var helpLeftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpRightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpReadOnlyLeftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var helpReadOnlyRightWidthConstraint: NSLayoutConstraint!
    
    @IBOutlet var helpView: HelpView!
    @IBOutlet var readOnlyHelpView: HelpView!
    @IBAction func helpPressed(_ sender: Any) {
        if !readOnly {
            self.helpView.show(owner: self)
        } else {
            self.readOnlyHelpView.show(owner: self)
        }
    }
    
    var readOnly: Bool = false
    
    var dragAndDropManager: DragAndDropManager?
    var tagSizingCell: TagCell?
    var categorySizingCell: CategoryCell?
    
    var fetchMoreTags: Bool = false
    var selectedCategory: Category!
    
    var tags: [Tag] = []
    var categories: [Category] = []
    var tagQuery: TagQuery = TagQuery()
    
    var _profile : Profile!
    var profile : Profile {
        set {
            _profile = newValue
            
            let text = NSMutableAttributedString(
                string: profile.name,
                attributes: [NSAttributedStringKey.foregroundColor: AccentColor])
           
            var separator = "\n"
            if profile.relationType != .none {
                text.append(NSMutableAttributedString(
                    string: separator + Emoji.relationType + profile.relationType.rawValue.codeLocalized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
                separator = SeparatorString
            }
            
            if let matchMode = profile.matchMode {
                text.append(NSMutableAttributedString(
                    string: separator + Emoji.matchMode + matchMode.description.codeLocalized,
                    attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                                 NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            }
            let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
            label.backgroundColor = UIColor.clear
            label.numberOfLines = 2
            label.font = UIFont.boldSystemFont(ofSize: 16.0)
            label.textAlignment = .center
            label.textColor = UIColor.white
            label.attributedText = text
            self.navigationItem.titleView = label

            _ = self.view
            updateCollectionViewLayout()
            setCategories()
            checkTagsExist()
        }
        get { return _profile }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.automaticallyAdjustsScrollViewInsets = false
        
        self.navigationBarBackground.alpha = 0
        self.searchBarBackground.alpha = 0
        
        if #available(iOS 11.0, *) {
            BottomSafeInsetX = UIApplication.shared.keyWindow?.safeAreaInsets.bottom ?? 0
            BottomBarHeightX = BottomBarHeight + BottomSafeInsetX
        }
        
        var edgeInsetTop = NavBarHeight + InfoBarHeight
        if #available(iOS 11.0, *) {
            edgeInsetTop = InfoBarHeight
        }
        
        matchButton.setTitleTextAttributes([NSAttributedStringKey.font: UIFont.systemFont(ofSize: 23.0)], for: .normal)
        matchButton.setTitleTextAttributes([NSAttributedStringKey.font: UIFont.systemFont(ofSize: 23.0)], for: .highlighted)
        matchButton.setTitleTextAttributes([NSAttributedStringKey.font: UIFont.systemFont(ofSize: 23.0)], for: .disabled)
        matchButton.setTitleTextAttributes([NSAttributedStringKey.font: UIFont.systemFont(ofSize: 23.0)], for: .selected)
        
        if isIPhoneX() {
            headerViewHeightConstraint.constant = StatusBarHeight + NavBarHeight + InfoBarHeight
            bottomViewHeightConstraint.constant = BottomBarHeightX
            categoryCollectionView.contentInset = UIEdgeInsets(top: 0, left: 5, bottom: 0, right: 5)
        }
        
        let tagCellNib = UINib(nibName: "TagCell", bundle:nil)
        self.posTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.posTagCollectionView.contentInset =
            UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight, right: 0)
        self.posTagCollectionView.suppressDragAndScroll = true
        self.posTagCollectionView.suppressReloadData = true
        self.posTagCollectionView.delegate = self
        self.posTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.negTagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.negTagCollectionView.contentInset =
            UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight, right: 0)
        self.negTagCollectionView.suppressDragAndScroll = true
        self.negTagCollectionView.suppressReloadData = true
        self.negTagCollectionView.delegate = self
        self.negTagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        
        self.tagCollectionView.register(tagCellNib, forCellWithReuseIdentifier: "TagCell")
        self.tagCollectionView.contentInset =
            UIEdgeInsets(top: InfoBarHeight, left: 0, bottom: BottomBarHeight, right: 0)
        self.tagCollectionView.suppressDragAndScroll = true
        self.tagCollectionView.suppressInsideDragAndDrop = true
        self.tagCollectionView.suppressReloadData = true
        self.tagCollectionView.delegate = self
        self.tagCollectionView.addObserver(self, forKeyPath: "contentSize", options: .initial, context: nil)
        self.tagSizingCell = (tagCellNib.instantiate(withOwner: nil, options: nil) as NSArray).firstObject as! TagCell?
        
        let categoryCellNib = UINib(nibName: "CategoryCell", bundle:nil)
        self.categoryCollectionView.register(categoryCellNib, forCellWithReuseIdentifier: "CategoryCell")
        self.categorySizingCell = (categoryCellNib.instantiate(withOwner: nil, options: nil) as NSArray).firstObject as! CategoryCell?
        
        self.dragAndDropManager = DragAndDropManager(canvas: self.view, collectionViews: [posTagCollectionView, negTagCollectionView, tagCollectionView])
        self.dragAndDropManager?.delegate = self
        
        NotificationCenter.default.addObserver(self, selector: #selector(setCategories), name: FavoriteCategoriesFetchedNotification, object: nil)
        
        if Settings.instance.disableNewTags {
            helpNewTagArrow.isHidden = true
            helpNewTagLabel.isHidden = true
            if let index = self.navigationItem.rightBarButtonItems?.index(of: newTagButton) {
                self.navigationItem.rightBarButtonItems?.remove(at: index)
            }
        }
        
        helpDropTagsTopicsLikeLabel.text = "DropTagsTopicsLike".termLocalized(Emoji.like)
        if helpDropTagsTopicsLikeLabel.text!.isEmpty {
            helpDropTagsTopicsLikeArrow.isHidden = true
        }
        helpDropTagsTopicsDislikeLabel.text = "DropTagsTopicsDislike".termLocalized(Emoji.dislike)
        if helpDropTagsTopicsDislikeLabel.text!.isEmpty {
            helpDropTagsTopicsDislikeArrow.isHidden = true
        }
        helpShowsTagsLikeLabel.text = "ShowsTagsLike".termLocalized(Emoji.like)
        if helpShowsTagsLikeLabel.text!.isEmpty {
            helpShowsTagsLikeArrow.isHidden = true
        }
        helpShowsTagsDislikeLabel.text = "ShowsTagsDislike".termLocalized(Emoji.dislike)
        if helpShowsTagsDislikeLabel.text!.isEmpty {
            helpShowsTagsDislikeArrow.isHidden = true
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.removeObserver(self, name: NSNotification.Name.UIKeyboardWillHide, object: nil)
    }
    
    deinit {
        self.posTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        self.negTagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        self.tagCollectionView.removeObserver(self, forKeyPath: "contentSize", context: nil)
        NotificationCenter.default.removeObserver(self)
    }
    
    func updateCollectionViewLayout() {
        if self.profile.effectiveMatchMode == .basic {
            leftWidthConstraint = leftWidthConstraint.setMultiplier(multiplier: 1.0)
            leftLabelWidthConstraint = leftLabelWidthConstraint.setMultiplier(multiplier: 1.0)
            rightWidthConstraint = rightWidthConstraint.setConstant(constant: 0.0)
            rightLabelWidthConstraint = rightLabelWidthConstraint.setConstant(constant: 0.0)
            separatorLine.isHidden = true
            helpLeftWidthConstraint = helpLeftWidthConstraint.setMultiplier(multiplier: 1.0)
            helpRightWidthConstraint = helpRightWidthConstraint.setConstant(constant: 0.0)
            helpReadOnlyLeftWidthConstraint = helpReadOnlyLeftWidthConstraint.setMultiplier(multiplier: 1.0)
            helpReadOnlyRightWidthConstraint = helpReadOnlyRightWidthConstraint.setConstant(constant: 0.0)
        }
    }
    
    func makeReadOnly() {
        _ = view
        if let index = self.navigationItem.rightBarButtonItems?.index(of: newTagButton) {
            self.navigationItem.rightBarButtonItems?.remove(at: index)
        }
        if let index = self.navigationItem.rightBarButtonItems?.index(of: matchButton) {
            self.navigationItem.rightBarButtonItems?.remove(at: index)
        }
        
        var edgeInsetTop = NavBarHeight + InfoBarHeight
        if #available(iOS 11.0, *) {
            edgeInsetTop = InfoBarHeight
        }
        
        self.dragAndDropManager!.makeReadOnly()
        self.tagActivityIndicator.isHidden = true
        self.categoryActivityIndicator.isHidden = true
        self.categoryActivityIndicator.stopAnimating()
        self.tagCollectionView.isHidden = true
        self.searchContainerView.isHidden = true
        self.categoryCollectionView.isHidden = true
        if UserData.instance.status.isEmpty {
            self.topSpaceConstraint.constant = 0
            self.middleHeightConstraint.constant = 0
            self.bottomHeightConstraint = self.bottomHeightConstraint.setMultiplier(multiplier: 0.0)
            self.posTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: 0, right: 0)
            self.negTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: 0, right: 0)
            self.helpStatusArrow.isHidden = true
            self.helpStatusLabel.isHidden = true
        } else {
            self.statusLabel.text = String(format: "»%@«".localized, UserData.instance.status)
            self.statusLabel.isHidden = false
            if isIPhoneX() {
                self.topSpaceConstraint.constant = -BottomBarHeightX
                self.middleHeightConstraint.constant = -BottomBarHeightX
                self.bottomHeightConstraint = self.bottomHeightConstraint.setConstant(constant: BottomBarHeightX)
                self.posTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight, right: 0)
                self.negTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: InfoBarHeight , right: 0)
            } else {
                self.topSpaceConstraint.constant = -BottomBarHeight
                self.middleHeightConstraint.constant = -BottomBarHeight
                self.bottomHeightConstraint = self.bottomHeightConstraint.setConstant(constant: BottomBarHeight)
                self.posTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: BottomBarHeight, right: 0)
                self.negTagCollectionView.contentInset = UIEdgeInsets(top: edgeInsetTop, left: 0, bottom: BottomBarHeight, right: 0)
            }
        }
        
        let text = NSMutableAttributedString(
            string: profile.name,
            attributes: [NSAttributedStringKey.foregroundColor: AccentColor])
        
        var separator = "\n"
        if profile.relationType != .none {
            text.append(NSMutableAttributedString(
                string: separator + Emoji.relationType + profile.relationType.rawValue.codeLocalized,
                attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                             NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
            separator = SeparatorString
        }
        
        text.append(NSMutableAttributedString(
            string: separator + Emoji.matchMode + profile.effectiveMatchMode.description.codeLocalized,
            attributes: [NSAttributedStringKey.foregroundColor: UIColor.black,
                         NSAttributedStringKey.font: UIFont.systemFont(ofSize: 12.0)]))
        
        let label = UILabel(frame: CGRect(x:0, y:0, width:200, height:50))
        label.backgroundColor = UIColor.clear
        label.numberOfLines = 2
        label.font = UIFont.boldSystemFont(ofSize: 16.0)
        label.textAlignment = .center
        label.textColor = UIColor.white
        label.attributedText = text
        self.navigationItem.titleView = label

        
        self.readOnly = true
        self.view.layoutIfNeeded()
    }
    
    @objc func setCategories() {
        self.updateLabels()
        if !DataService.instance.favoriteCategoriesLoaded {
            return
        }
        
        categories = []
        categories.append(CategoryFavorite)
        categories.append(CategorySearch)
        categories.append(CategoryOwn)
        categories.append(CategoryStaged)
        categories.append(CategoryMore)
        categories.append(contentsOf: DataService.instance.favoriteCategories)
        
        if DataService.instance.favoriteCategoriesLoaded && !self.categoryActivityIndicator.isHidden {
            UIView.animate(withDuration: 0.1, animations: {
                self.categoryActivityIndicator.alpha = 0.0
            }, completion: { (completed) in
                self.categoryActivityIndicator.isHidden = true
                self.categoryActivityIndicator.stopAnimating()
            })
        }
        
        for category in categories {
            category.selected = false
        }
        selectedCategory = categories[0]
        selectedCategory.selected = true
        
        self.refreshCategories()
        tagQuery.favorite = true
        self.fetchTags()
    }
    
    func checkTagsExist() {
        if !NoTagAlertShown {
            _ = DataService.instance.hasTags().then { (exists) -> () in
                if !exists {
                    let alertController = UIAlertController(title: "Language Information".localized, message: "NoTagsForLanguage".aliasLocalized, preferredStyle: .alert)
                    let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
                    alertController.addAction(okAction)
                    alertController.view?.tintColor = AccentColor
                    self.present(alertController, animated: true, completion: nil)
                    NoTagAlertShown = true
                }
            }.catch { (error) in
                let alertController = UIAlertController(title: "Network Error".localized, message: "UnexpectedErrorOccurred".aliasLocalized, preferredStyle: .alert)
                let okAction = UIAlertAction(title: "OK".localized, style: .default, handler: nil)
                alertController.addAction(okAction)
                alertController.view?.tintColor = AccentColor
                self.present(alertController, animated: true, completion: nil)
            }
        }
    }
    
    func refreshCategories() {
        self.updateLabels()
        self.categoryCollectionView.reloadSections(IndexSet(integer: 0))
    }

    func clearTags() {
        self.tags.removeAll()
        self.refreshTags()
    }
    
    func fetchTags() {
        clearTags()
        let searchText = tagQuery.searchText.searchNormalized(langCode: UserData.instance.langCode)
        if !tagQuery.own {
            if !tagQuery.search || !searchText.isEmpty {
                self.tagActivityIndicator.startAnimating()
                UIView.animate(withDuration: 0.5) {
                    self.tagActivityIndicator.alpha = 1.0
                }
                _ = DataService.instance.fetchTags(tagQuery).then { (tags) -> () in
                    self.addTags(tags)
                }
            }
        } else {
            for (_, tag) in UserData.instance.ownTags {
                if tag.space == SecureStore.spaceRefName &&
                    tag.langCode == UserData.instance.langCode &&
                    (searchText.isEmpty || tag.search.hasPrefix(searchText)) {
                    tags.append(tag)
                }
            }
            tags.sort(by: {
                return $0.name < $1.name
            })
            self.addTags(tags)
        }
    }
    
    func addTags(_ tags: [Tag]) {
        UIView.animate(withDuration: 0.1, animations: {
            self.tagActivityIndicator.alpha = 0.0
        }, completion: { (completed) in
            self.tagActivityIndicator.stopAnimating()
        })
        self.tags = tags.filter {
            let key = $0.effectiveKey
            return !(self.profile.posTags.contains {
                $0.effectiveKey == key
                } || self.profile.negTags.contains {
                    $0.effectiveKey == key
                })
        }
        self.refreshTags()
    }
    
    func refreshTags() {
        self.updateLabels()
        self.tagCollectionView.reloadData()
        self.fetchMoreTags = false
    }
    
    func refreshPosNegTags() {
        self.updateLabels()
        self.posTagCollectionView.reloadData()
        self.negTagCollectionView.reloadData()
        fetchTags()
    }
    
    func tagsForCollectionView(_ collectionView: UICollectionView) -> [Tag] {
        switch collectionView {
            case tagCollectionView!:
                return tags
            case posTagCollectionView!:
                return profile.posTags
            case negTagCollectionView!:
                return profile.negTags
            default:
                return []
        }
    }
    
    func insertTagForCollectionView(_ collectionView: UICollectionView, tag: Tag, indexPath : IndexPath) {
        switch collectionView {
            case tagCollectionView!:
                break
            case posTagCollectionView!:
                profile.posTags.insert(tag, at: indexPath.item)
            case negTagCollectionView!:
                profile.negTags.insert(tag, at: indexPath.item)
            default:
                return
        }
    }
    
    func removeTagForCollectionView(_ collectionView: UICollectionView, indexPath : IndexPath) {
        switch collectionView {
            case tagCollectionView!:
                tags.remove(at: indexPath.item)
            case posTagCollectionView!:
                profile.posTags.remove(at: indexPath.item)
            case negTagCollectionView!:
                profile.negTags.remove(at: indexPath.item)
            default:
                return
        }
    }
    
    func indexPathForTag(_ collectionView: UICollectionView, tag: Tag) -> IndexPath? {
        switch collectionView {
            case tagCollectionView!:
                return nil
            case posTagCollectionView!:
                if let index = profile.posTags.index(of: tag) {
                    return IndexPath(row: index, section: 0)
                }
                return nil
            case negTagCollectionView!:
                if let index = profile.negTags.index(of: tag) {
                    return IndexPath(row: index, section: 0)
                }
                return nil
            default:
                return nil
        }
    }
    
    // MARK: UICollectionViewDataSource
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        if collectionView == categoryCollectionView! {
            return categories.count
        } else {
            return tagsForCollectionView(collectionView).count
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if collectionView == categoryCollectionView! {
            
            let categoryCell = collectionView.dequeueReusableCell(withReuseIdentifier: "CategoryCell", for: indexPath) as! CategoryCell
            let category = categories[indexPath.item]
            categoryCell.delegate = self
            categoryCell.categoryView.button.tag = indexPath.item
            categoryCell.data = category
            return categoryCell
            
        } else {
            
            let tagCell = collectionView.dequeueReusableCell(withReuseIdentifier: "TagCell", for: indexPath) as! TagCell
            let tags = tagsForCollectionView(collectionView)
            if indexPath.item < tags.count {
                let tag = tags[indexPath.item]
                tagCell.data = tag
                tagCell.isHidden = false
                
                if let collectionView = collectionView as? DragAndDropCollectionView {
                    if let draggingPathOfCellBeingDragged = collectionView.draggingPathOfCellBeingDragged {
                        if draggingPathOfCellBeingDragged.item == indexPath.item {
                            tagCell.isHidden = true
                        }
                    }
                }

                // Restrict tag count => Make transparent
                /*
                tagCell.alpha = 1.0
                if !dragAndDropManager!.isDragging && (collectionView == posTagCollectionView || collectionView == negTagCollectionView) {
                    if indexPath.row >= MaxTagCount {
                        tagCell.alpha = MaxTagTransparent
                    }
                }
                */
            }
            return tagCell
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        if collectionView == categoryCollectionView! {
            let category = categories[indexPath.item]
            if category.mark > 0 {
                return CGSize(width: ContentSize + 2 * 4, height: ContentSize)
            } else {
                self.categorySizingCell?.categoryView.button.setTitle(category.name + IconPlaceholder, for: .normal)
                return CGSize(width: self.categorySizingCell!.categoryView.systemLayoutSizeFitting(UILayoutFittingCompressedSize).width, height: ContentSize)
            }
        } else {
            var maxWidth = TagMaxWidth
            if self.profile.effectiveMatchMode != .basic && (collectionView == posTagCollectionView! || collectionView == negTagCollectionView!) {
                maxWidth = TagMaxHalfWidth
            }
            let tags = tagsForCollectionView(collectionView)
            if indexPath.item < tags.count {
                let tag = tags[indexPath.item]
                self.tagSizingCell?.tagView.button.setTitle(tag.name + IconPlaceholder, for: .normal)
                return CGSize(width: min(self.tagSizingCell!.tagView.systemLayoutSizeFitting(UILayoutFittingCompressedSize).width, maxWidth), height: ContentSize)
            }
            return .zero
        }
    }
    
    // MARK: DragAndDropCollectionViewDataSource
    func collectionView(_ collectionView: UICollectionView, dataItemForIndexPath indexPath: IndexPath) -> AnyObject {
        return tagsForCollectionView(collectionView)[indexPath.item]
    }
    func collectionView(_ collectionView: UICollectionView, insertDataItem dataItem : AnyObject, atIndexPath indexPath: IndexPath) -> Void {
        if let tag = dataItem as? Tag {
            insertTagForCollectionView(collectionView, tag: tag, indexPath: indexPath)
        }
    }
    func collectionView(_ collectionView: UICollectionView, deleteDataItemAtIndexPath indexPath : IndexPath) -> Void {
        removeTagForCollectionView(collectionView, indexPath: indexPath)
    }
    
    func collectionView(_ collectionView: UICollectionView, moveDataItemFromIndexPath from: IndexPath, toIndexPath to : IndexPath) -> Void {
        let tag: Tag = tagsForCollectionView(collectionView)[from.item]
        removeTagForCollectionView(collectionView, indexPath: from)
        insertTagForCollectionView(collectionView, tag: tag, indexPath: to)
    }
    
    func collectionView(_ collectionView: UICollectionView, indexPathForDataItem dataItem: AnyObject) -> IndexPath? {
        if let tag = dataItem as? Tag {
            let tags = tagsForCollectionView(collectionView)
            for aTag : Tag in tags {
                if tag == aTag {
                    let position = tags.index(of: aTag)!
                    let indexPath = IndexPath(item: position, section: 0)
                    return indexPath
                }
            }
        }
        return nil
    }
    
    // MARK: DragAndDropManagerDelegate
    func didBeginDrag(_ view: UIView, item : AnyObject) -> Void {
        self.posTagCollectionView.reloadData()
        self.negTagCollectionView.reloadData()
        self.tagCollectionView.reloadData()
    }
    
    func didEndDrop(_ view: UIView, sourceView: UIView, item : AnyObject, didMoveOut: Bool) -> Void {
        let tag = item as! Tag
        var previousValue : Int = 0
        if sourceView == tagCollectionView {
            previousValue = 0
        } else if sourceView == posTagCollectionView {
            previousValue = 1
        } else if sourceView == negTagCollectionView {
            previousValue = -1
        }
        if view == tagCollectionView {
            if didMoveOut {
                fetchTags()
            } else {
                self.refreshTags()
            }
            Analytics.instance.logNeutral(tag: tag, previousValue: previousValue)
        } else if view == posTagCollectionView {
            Analytics.instance.logPositive(tag: tag, previousValue: previousValue)
        } else if view == negTagCollectionView {
            Analytics.instance.logNegative(tag: tag, previousValue: previousValue)
        }
        self.updateLabels()
        self.posTagCollectionView.reloadData()
        self.negTagCollectionView.reloadData()
        self.profile.touch()
        if let tag = item as? Tag {
            let collectionView = view as! UICollectionView
            if let indexPath = indexPathForTag(view as! UICollectionView, tag: tag) {
                if indexPath.row >= collectionView.numberOfItems(inSection: 0) - 1 {
                    (view as! UICollectionView).scrollToItem(at: indexPath, at: .top, animated: true)
                }
            }
        }
    }
    
    func updateLabels() {
        posTagLabel.text = String(format: "LikeNum".aliasLocalized, Emoji.like, profile.posTags.count)
        negTagLabel.text = String(format: "DislikeNum".aliasLocalized, Emoji.dislike, profile.negTags.count)
        
        if selectedCategory == nil {
            tagLabel.text = "ChooseFromDots".aliasLocalized
        } else if selectedCategory.mark > 0 {
            if selectedCategory.mark == CategoryFavorite.mark {
                tagLabel.text = "ChooseFromFavorites".aliasLocalized
            } else if selectedCategory.mark == CategorySearch.mark {
                tagLabel.text = "ChooseFromSearch".aliasLocalized
            } else if selectedCategory.mark == CategoryOwn.mark {
                tagLabel.text = "ChooseFromOwn".aliasLocalized
            } else if selectedCategory.mark == CategoryStaged.mark {
                tagLabel.text = "ChooseFromUncategorized".aliasLocalized
            } else if selectedCategory.mark == CategoryMore.mark {
                tagLabel.text = "ChooseFromDots".aliasLocalized
            }
        } else {
            tagLabel.text = String(format: "ChooseFromCategory".aliasLocalized, selectedCategory.name)
        }
    }
    
    // MARK: Search
    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
        tagQuery.searchText = searchText
        fetchTags()
    }
    
    func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        cancelSearching()
    }
    
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        self.view.endEditing(true)
    }
    
    func searchBarTextDidBeginEditing(_ searchBar: UISearchBar) {
        self.searchBar!.setShowsCancelButton(true, animated: true)
    }
    
    func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
        self.searchBar!.setShowsCancelButton(false, animated: false)
    }
    
    func cancelSearching(){
        self.searchBar!.resignFirstResponder()
    }
    
    // MARK: CategoryCellDelegate
    func didPressCategory(_ button : UIButton) -> Void {
        let category = categories[button.tag]
        selectCategory(category)
    }
    
    func selectCategory(_ category: Category) {
        if category == selectedCategory && category.mark != CategoryMore.mark {
            return
        }

        searchBar?.resignFirstResponder()
        
        for category in categories {
            category.selected = false
        }
        selectedCategory = category
        selectedCategory.selected = true
        
        let searchHidden = selectedCategory.mark == CategoryFavorite.mark
        
        searchBar?.text = ""
        if !searchHidden {
            if selectedCategory.mark == CategorySearch.mark {
                searchBar?.placeholder = "Search".localized
            } else {
                searchBar?.placeholder = "Filter".localized
            }
        }
        searchBar.isUserInteractionEnabled = !searchHidden
        
        self.tagQuery.searchText = ""
        self.tagQuery.favorite = selectedCategory.mark == CategoryFavorite.mark
        self.tagQuery.search = selectedCategory.mark == CategorySearch.mark
        self.tagQuery.own = selectedCategory.mark == CategoryOwn.mark
        self.tagQuery.categoryStaged = selectedCategory.mark == CategoryStaged.mark
        self.tagQuery.categoryKey = selectedCategory.mark == 0 ? selectedCategory.key : ""
        
        let barHeight = InfoBarHeight + (searchHidden ? 0 : NavBarHeight - BarSpace)
        let updateContent = {
            self.searchBar.alpha = searchHidden ? 0.0 : 1.0
            self.tagCollectionView.contentInset = UIEdgeInsets(top: barHeight, left: 0, bottom: BottomBarHeight, right: 0)
            self.scrollToTop()
            if self.selectedCategory.mark == CategoryMore.mark {
                self.clearTags()
                self.openCategorySearch()
            } else {
                self.fetchTags()
                if self.selectedCategory.mark == CategorySearch.mark {
                    self.searchBar.becomeFirstResponder()
                }
            }
        }
        
        refreshCategories()
        if searchContainerConstraint.constant != barHeight {
            searchContainerConstraint.constant = barHeight
            UIView.animate(withDuration: 0.2) {
                self.view.layoutIfNeeded()
                updateContent()
            }
        } else {
            updateContent()
        }
    }
    
    func scrollToTop() {
        tagCollectionView.setContentOffset(CGPoint(x: 0, y: -tagCollectionView.contentInset.top), animated: false)
    }
    
    // MARK: Keyboard Notifications
    @objc func keyboardWillShow(sender: NSNotification) {
        let info = sender.userInfo!
        let duration: TimeInterval = (info[UIKeyboardAnimationDurationUserInfoKey] as! NSNumber).doubleValue
        let keyboardSize = (info[UIKeyboardFrameEndUserInfoKey] as! NSValue).cgRectValue.height
        viewBottomConstraint.constant = keyboardSize - bottomLayoutGuide.length
        if isIPhoneX() {
            self.tagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight + NavBarHeight - BarSpace, left: 0, bottom: BottomBarHeightX, right: 0)
        }
        UIView.animate(withDuration: duration) {
            self.view.layoutIfNeeded()
        }
    }
    
    @objc func keyboardWillHide(sender: NSNotification) {
        let info = sender.userInfo!
        let duration: TimeInterval = (info[UIKeyboardAnimationDurationUserInfoKey] as! NSNumber).doubleValue
        if isIPhoneX() {
            self.tagCollectionView.contentInset = UIEdgeInsets(top: InfoBarHeight + NavBarHeight - BarSpace, left: 0, bottom: BottomBarHeight, right: 0)
        }
        viewBottomConstraint.constant = 0
        UIView.animate(withDuration: duration) {
            self.view.layoutIfNeeded()
        }
    }
    
    // MARK: UIScrollViewDelegate
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        if scrollView == tagCollectionView {
            if scrollView.contentOffset.y >= (scrollView.contentSize.height - scrollView.frame.size.height) - ScrollThreshold {
                if !fetchMoreTags {
                    fetchMoreTags = true
                    // DataService: Fetch more tags
                }
            }
        }
        DispatchQueue.main.async() {
            self.setScrollAlpha(scrollView)
        }
    }
    
    func setScrollAlpha(_ scrollView : UIScrollView? = nil) {
        if let scrollView = scrollView {
            if scrollView == posTagCollectionView || scrollView == negTagCollectionView {
                var contentOffsetY =
                    max(posTagCollectionView.contentInset.top + posTagCollectionView.contentOffset.y,
                        negTagCollectionView.contentInset.top + negTagCollectionView.contentOffset.y)
                if #available(iOS 11.0, *) {
                    if (isIPhoneX()) {
                        contentOffsetY = contentOffsetY + NavBarHeight + StatusBarDelta
                    } else {
                        contentOffsetY = contentOffsetY + NavBarHeight
                    }
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
        let contentOffsetY =
            max(tagCollectionView.contentInset.top + tagCollectionView.contentOffset.y,
                max(posTagCollectionView.contentSize.height + posTagCollectionView.contentInset.bottom -
                    posTagCollectionView.contentOffset.y - posTagCollectionView.frame.size.height,
                    negTagCollectionView.contentSize.height + negTagCollectionView.contentInset.bottom -
                    negTagCollectionView.contentOffset.y - negTagCollectionView.frame.size.height))
        var alpha = contentOffsetY / BlurAlphaRatio
        if alpha < 0 {
            alpha = 0
        }
        if alpha > 1 {
            alpha = 1
        }
        searchBarBackground.alpha = alpha
    }
    
    func inviteFriendAlert(_ completion : (() -> ())? = nil) {
        let title = "ActivateOwnTags".aliasLocalized
        let message = UserData.instance.inviteFriendSentCount == 0 ? "InviteFriends1".aliasLocalized : "InviteFriends2".aliasLocalized
        let alertController = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alertController.isModalInPopover = true
        
        let mailAction = UIAlertAction(title: "Write Mail".localized, style: .default, handler: { (action : UIAlertAction) in
            Mail.instance.sendInviteFriendsMail(presenter: self, completion: { (result) in
                if result == MFMailComposeResult.sent {
                    let delayTime = DispatchTime.now() + 1.0
                    DispatchQueue.main.asyncAfter(deadline: delayTime) {
                        if let completion = completion {
                            completion()
                        }
                    }
                }
            })
        })
        alertController.addAction(mailAction)
        
        let messageAction = UIAlertAction(title: "Send Message".localized, style: .default, handler: { (action : UIAlertAction) in
            Mail.instance.sendInviteFriendsMessage(presenter: self, completion: { (result) in
                if result == MessageComposeResult.sent {
                    let delayTime = DispatchTime.now() + 1.0
                    DispatchQueue.main.asyncAfter(deadline: delayTime) {
                        if let completion = completion {
                            completion()
                        }
                    }
                }
            })
        })
        alertController.addAction(messageAction)
        
        let skipAction = UIAlertAction(title: "Skip Once".localized, style: .default, handler: { (action : UIAlertAction) in
            if let completion = completion {
                completion()
            }
        })
        alertController.addAction(skipAction)
        
        let cancelAction = UIAlertAction(title: "Cancel".localized, style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        alertController.view.tintColor = AccentColor
        self.present(alertController, animated: true, completion: nil)
    }
    
    func didCreateNewTag(_ newTag: NewTag) {
        assignTag(newTag.tag, assignPos: newTag.assignPos)
    }
    
    func didUseExistingTag(_ tag: Tag, newTag: NewTag) {
        assignTag(tag, assignPos: newTag.assignPos)
    }
    
    func assignTag(_ tag: Tag, assignPos: Bool) {
        let posIndex = profile.posTags.index { (posTag) -> Bool in
            return posTag.effectiveKey == tag.effectiveKey
        }
        let negIndex = profile.negTags.index { (negTag) -> Bool in
            return negTag.effectiveKey == tag.effectiveKey
        }
        if assignPos {
            if posIndex == nil {
                profile.posTags.append(tag)
            }
            if let negIndex = negIndex {
                profile.negTags.remove(at: negIndex)
            }
        } else {
            if negIndex == nil {
                profile.negTags.append(tag)
            }
            if let posIndex = posIndex {
                profile.posTags.remove(at: posIndex)
            }
        }
        UserData.instance.addOwnTag(tag)
        profile.touch()
        self.refreshPosNegTags()
        let dispatchTime = DispatchTime.now() + 0.5
        DispatchQueue.main.asyncAfter(deadline: dispatchTime) {
            if assignPos {
                if let indexPath = self.indexPathForTag(self.posTagCollectionView, tag: tag) {
                    self.posTagCollectionView.scrollToItem(at: indexPath, at: .top, animated: true)
                }
            } else {
                if let indexPath = self.indexPathForTag(self.negTagCollectionView, tag: tag) {
                    self.negTagCollectionView.scrollToItem(at: indexPath, at: .top, animated: true)
                }
            }
        }
    }
    
    func openCategorySearch() {
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let searchCategoryController = storyboard.instantiateViewController(withIdentifier: "searchCategory") as! SearchCategoryViewController
        searchCategoryController.delegate = self
        searchCategoryController.isPresentedModal = true
        let navigationController = UINavigationController(rootViewController: searchCategoryController)
        self.present(navigationController, animated: true, completion: nil)
    }
    
    func didSelectCategory(_ category: Category, newCategory: NewCategory?) {
        self.selectedCategory = category
        self.tagQuery.categoryKey = category.key
        self.refreshCategories()
        self.fetchTags()
    }
    
    // MARK: Segue
    override func shouldPerformSegue(withIdentifier identifier: String, sender: Any?) -> Bool {
        if identifier == "addTag" {
            if UserData.instance.inviteFriendSentCount < 2 {
                DispatchQueue.main.async() {
                    self.inviteFriendAlert({
                        self.performSegue(withIdentifier: "addTagForce", sender: sender)
                    })
                }
                return false
            }
        }
        return true
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "sayhi",
            let qrCodeViewController = (segue.destination as? UINavigationController)?.topViewController as? QRCodeViewController {
            qrCodeViewController.view.frame = self.view.bounds
            qrCodeViewController.profile = profile
            qrCodeViewController.hideBarButtonItems(["tag"])
        } else if segue.identifier == "addTag" || segue.identifier == "addTagForce",
            let newTagTableController = (segue.destination as? UINavigationController)?.topViewController as? NewTagTableController {
            newTagTableController.delegate = self
            newTagTableController.contentLangCode = UserData.instance.langCode
        }
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        setScrollAlpha()
    }
}
