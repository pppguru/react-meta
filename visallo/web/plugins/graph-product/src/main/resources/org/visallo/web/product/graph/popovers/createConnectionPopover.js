
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/withFormFieldErrors',
    'util/withTeardown',
    'util/withDataRequest',
    'util/visibility/edit',
    'detail/dropdowns/propertyForm/justification'
], function(
    defineComponent,
    withVertexPopover,
    withFormFieldErrors,
    withTeardown,
    withDataRequest,
    Visibility,
    Justification) {
    'use strict';

    return defineComponent(
        CreateConnectionPopover,
        withVertexPopover,
        withFormFieldErrors,
        withTeardown,
        withDataRequest
    );

    function CreateConnectionPopover() {

        this.defaultAttrs({
            connectButtonSelector: '.connect-dialog .btn-primary',
            invertButtonSelector: '.connect-dialog button.invert-connection'
        });

        this.before('teardown', function() {
            this.trigger('finishedVertexConnection');
        })

        this.before('initialize', function(node, config) {
            config.template = 'createConnectionPopover';
        });

        this.after('initialize', function() {
            this.on('click', {
                connectButtonSelector: this.onCreateConnection,
                invertButtonSelector: this.onInvert
            });
        });

        this.popoverInitialize = function() {
            this.visibilitySource = null;
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);

            this.updateRelationshipLabels();
        };

        this.updateRelationshipLabels = function() {
            var self = this,
                select = this.popover.find('select'),
                button = this.select('connectButtonSelector');

            select.html('<option>' + i18n('popovers.connection.loading') + '</option>');
            button.text(i18n('popovers.connection.button.connect')).attr('disabled', true);

            this.getRelationshipLabels(
                this.attr.otherCyNode,
                this.attr.cyNode
            ).then(function(relationships) {

                if (relationships.length) {
                    select.html(
                        relationships.map(function(d) {
                            return '<option value="' + d.title + '">' + d.displayName + '</option>';
                        }).join('')
                    );

                    Visibility.attachTo(self.popover.find('.visibility'), {
                        value: ''
                    });
                    Justification.attachTo(self.popover.find('.justification'));
                    self.positionDialog();
                    self.checkValid();
                } else {
                    select.html('<option>' + i18n('relationship.form.no_valid_relationships') + '</option>');
                }

                self.positionDialog();
            }).catch(function() {
                select.html('<option>' + i18n('popovers.connection.error') + '</option>');
            })
        }

        this.getTemplate = function() {
            return new Promise(f => require(['./createConnectionPopoverTpl'], f));
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onJustificationChange = function(event, data) {
            this.justification = data;
            this.checkValid();
        };

        this.checkValid = function() {
            var button = this.select('connectButtonSelector'),
                select = this.popover.find('select');

            if (select.val() &&
                this.visibilitySource && this.visibilitySource.valid &&
                this.justification && this.justification.valid) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }
        }

        this.onInvert = function(e) {
            var self = this;

            if (this.ignoreViewportChanges) {
                return;
            }

            if (!this.currentNodeIndex) {
                this.currentNodeIndex = 1;
                this.nodes = [this.attr.cyNode, this.attr.otherCyNode];
            }

            var node = this.nodes[this.currentNodeIndex % 2],
                other = this.nodes[(this.currentNodeIndex + 1) % 2],
                otherTitle = other.data('truncatedTitle'),
                currentNodePosition = node.renderedPosition(),
                otherNodePosition = other.renderedPosition();

            this.currentNodeIndex++;

            this.ignoreViewportChanges = true;
            this.popover.find('.title').text(otherTitle);
            this.attr.cyNode = node;
            this.attr.otherCyNode = other;
            this.updateRelationshipLabels();

            this.attr.cy.animate({
                panBy: {
                    x: otherNodePosition.x - currentNodePosition.x,
                    y: otherNodePosition.y - currentNodePosition.y
                }
            }, {
                duration: 400,
                easing: 'spring(250, 20)',
                complete: function() {
                    self.ignoreViewportChanges = false;
                    self.onViewportChanges();
                }
            });

            this.trigger(document, 'invertVertexConnection');
        };

        this.onCreateConnection = function(e) {
            var self = this,
                $target = $(e.target)
                    .text('Connecting...')
                    .attr('disabled', true),
                parameters = {
                    outVertexId: this.attr.outVertexId,
                    inVertexId: this.attr.inVertexId,
                    predicateLabel: $target.siblings('select').val(),
                    visibilitySource: this.visibilitySource.value
                },
                inputs = this.popover.find('select, input')
                    .attr('disabled', true);

            if (this.attr.otherCyNode.id() !== this.attr.edge.data('source')) {
                // Invert
                parameters.outVertexId = this.attr.inVertexId;
                parameters.inVertexId = this.attr.outVertexId;
            }

            this.attr.teardownOnTap = false;

            if (this.justification.sourceInfo) {
                parameters.sourceInfo = JSON.stringify(this.justification.sourceInfo);
            } else if (this.justification.justificationText) {
                parameters.justificationText = this.justification.justificationText;
            }

            this.dataRequest('edge', 'create', parameters)
                .then(function(data) {
                    self.teardown();
                })
                .catch(function(error) {
                    $target.text(i18n('popovers.connection.button.connect'))
                        .add(inputs)
                        .removeAttr('disabled');
                    self.markFieldErrors(error);
                    self.positionDialog();
                })
                .finally(function() {
                    self.attr.teardownOnTap = true;
                })
        };

        this.getRelationshipLabels = function(source, dest) {
            var sourceConceptTypeId = source.data('conceptType'),
                destConceptTypeId = dest.data('conceptType');

            return Promise.all([
                this.dataRequest('ontology', 'relationshipsBetween', sourceConceptTypeId, destConceptTypeId),
                this.dataRequest('ontology', 'relationships')
            ]).then(function(results) {
                var relationships = results[0],
                    ontologyRelationships = results[1],
                    relationshipsTpl = [];

                relationships.forEach(function(relationship) {
                    var ontologyRelationship = ontologyRelationships.byTitle[relationship.title];
                    if (ontologyRelationship && ontologyRelationship.userVisible !== false) {
                        relationshipsTpl.push({
                            title: relationship.title,
                            displayName: ontologyRelationship.displayName
                        });
                    }
                });

                return relationshipsTpl;

            });
        };
    }
});
