define(['util/promise'], function() {
    'use strict';

    return {
        openSearchType: function(searchType, options) {
            var $searchPane = $('.search-pane');
            return new Promise(function(fulfill, reject) {
                    if ($searchPane.hasClass('visible')) {
                        fulfill();
                    } else {
                        $(document).on('searchPaneVisible', function handler(data) {
                            $(this).off('searchPaneVisible', handler);
                            fulfill();
                        }).trigger('menubarToggleDisplay', { name: 'search' });
                    }
                })
                .then(function() {
                    var currentType = null;
                    $searchPane.find('.content').lookupAllComponents().forEach(function(c) {
                        if (c.searchType) {
                            currentType = c.searchType;
                        }
                    });

                    var filtersLoaded = $searchPane.find('.search-filters').length,
                        advancedSearchActive = $searchPane.find('.search-type.active').length === 0;

                    if (advancedSearchActive ||
                        !filtersLoaded ||
                        !currentType ||
                        currentType !== searchType) {
                        return new Promise(function(f) {
                            $(document).on('searchtypeloaded', function loadedHandler(event) {
                                    $(this).off('searchtypeloaded', loadedHandler);
                                    f(event.target);
                                })
                                .trigger('switchSearchType', searchType);
                        })
                    }
                })
                .then(function(node) {
                    if (options && options.clearSearch === true) {
                        $searchPane
                            .find('.search-type-' + searchType.toLowerCase())
                            .trigger('clearSearch');
                    }

                    return node;
                })
        }
    }
})
