define([
    'util/popovers/withElementScrollingPositionUpdates',
    'util/withCollapsibleSections',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/privileges',
    'util/dnd',
    'require'
], function(
    withElementScrolling,
    withCollapsibleSections,
    F,
    withDataRequest,
    Privileges,
    dnd,
    require) {
    'use strict';

    return withItem;

    function withItem() {

        withElementScrolling.call(this);
        withCollapsibleSections.call(this);
        if (!_.isFunction(this.dataRequest)) {
            withDataRequest.call(this);
        }

        this.attributes({
            propertiesSelector: '.org-visallo-properties',
            relationshipsSelector: '.org-visallo-relationships',
            commentsSelector: '.org-visallo-comments',
            deleteFormSelector: '.delete-form'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('openFullscreen', this.onOpenFullscreen);
            if (!_.isArray(this.attr.model)) {
                this.on('addProperty', this.redirectToPropertiesComponent);
                this.on('deleteProperty', this.redirectToPropertiesComponent);
                this.on('addNewProperty', this.onAddNewProperty);
                this.on('addNewComment', this.onAddNewComment);
                this.on('deleteItem', this.onDeleteItem);
                this.on('openSourceUrl', this.onOpenSourceUrl);
                this.on('maskWithOverlay', this.onMaskWithOverlay);
                this.on('commentOnSelection', this.onCommentOnSelection);
                this.on('addImage', this.onAddImage);
                this.on('openOriginal', this.onOpenOriginal);
                this.on('downloadOriginal', this.onDownloadOriginal);
                this.on('updateModel', this.onUpdateModel);

                this.makeVertexTitlesDraggable();
            }
        });

        this.onUpdateModel = function(event, data) {
            if (event.target === this.node) {
                this.attr.model = data.model;
            }
        };

        this.redirectToPropertiesComponent = function(event, data) {
            if ($(event.target).closest('.comments').length) {
                return;
            }

            if ($(event.target).closest(this.attr.propertiesSelector).length === 0) {
                event.stopPropagation();

                var properties = this.select('propertiesSelector');
                if (properties.length) {
                    _.defer(function() {
                        properties.trigger(event.type, data);
                    })
                } else {
                    throw new Error('Unable to redirect properties request', event.type, data);
                }
            }
        };

        this.onOpenOriginal = function(event) {
            window.open(F.vertex.raw(this.attr.model));
        };

        this.onDownloadOriginal = function(event) {
            var rawSrc = F.vertex.raw(this.attr.model);
            window.open(rawSrc + (
                /\?/.test(rawSrc) ? '&' : '?'
            ) + 'download=true');
        };

        this.onAddImage = function(event, data) {
            this.$node.find('.entity-glyphicon').trigger('setImage', data);
        };

        this.onCommentOnSelection = function(event, data) {
            var $comments = this.select('commentsSelector');

            if (!$(event.target).is($comments)) {
                $comments.trigger(event.type, data);
            }
        };

        this.makeVertexTitlesDraggable = function() {
            const model = this.attr.model;
            this.$node.find('.org-visallo-layout-header .vertex-draggable')
                .filter(function() {
                    if (!_.isEmpty($(this).attr('data-vertex-id'))) {
                        this.setAttribute('draggable', true)
                        return true
                    }
                    return false
                })
                .on('dragstart', function(e) {
                    const id = e.target.dataset.vertexId;
                    const elements = { vertexIds: [id], edgeIds: [] };
                    const dt = e.originalEvent.dataTransfer;

                    dt.effectAllowed = 'all';
                    if (model.id === id) {
                        const url = F.vertexUrl.url([model], visalloData.currentWorkspaceId);
                        dnd.setDataTransferWithElements(dt, { elements: [model] })
                    } else {
                        dnd.setDataTransferWithElements(dt, elements);
                    }
                })
        };

        this.onAddNewProperty = function(event) {
            this.trigger(this.select('propertiesSelector'), 'editProperty');
        };

        this.onAddNewComment = function(event) {
            this.trigger(this.select('commentsSelector'), 'editComment');
        };

        this.onDeleteItem = function(event) {
            var self = this,
                $container = this.select('deleteFormSelector');

            if ($container.length === 0) {
                $container = $('<div class="delete-form"></div>').insertBefore(
                    this.select('propertiesSelector')
                );
            }

            require(['../dropdowns/deleteForm/deleteForm'], function(DeleteForm) {
                var node = $('<div class="underneath"></div>').appendTo($container);
                DeleteForm.attachTo(node, {
                    data: self.attr.model
                });
            });
        };

        this.onOpenFullscreen = function(event, data) {
            event.stopPropagation();

            var viewing = this.attr.model,
                vertices = data && data.vertices ?
                    data.vertices :
                    _.isObject(viewing) && viewing.vertices ?
                    viewing.vertices :
                    viewing,
                url = F.vertexUrl.url(
                    _.isArray(vertices) ? vertices : [vertices],
                    visalloData.currentWorkspaceId
                );
            window.open(url);
        };

        this.onOpenSourceUrl = function(event, data) {
            window.open(data.sourceUrl);
        };

        this.onMaskWithOverlay = function(event, data) {
            event.stopPropagation();
            if (data.done) {
                this.$node.find('.detail-overlay').remove();
                this.trigger('selectObjects');
            } else {
                $('<div>')
                    .addClass('detail-overlay')
                    .toggleClass('detail-overlay-loading', data.loading)
                    .append($('<h1>').text(data.text))
                    .appendTo(this.$node);
            }
        };

    }
});
