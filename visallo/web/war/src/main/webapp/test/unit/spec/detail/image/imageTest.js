
describeComponent('detail/image/image', function() {

    var SRC = 'vertex/thumbnail?workspaceId=w1&graphVertexId=avertexid&width=800',
        EXPECTED_WIDTH = 364,
        VERTEX_ID = 'aVertexId',
        ROW_KEY = 'aRowKey',
        ROW_KEY_2 = 'aRowKey2',
        expectedPercent = function(x) {
            return x / EXPECTED_WIDTH * 100
        };

    beforeEach(function() {
        setupComponent(this, {
            model: {
                id: 'avertexid',
                type: 'vertex',
                properties: [
                    { name: 'http://visallo.org#conceptType', value: 'http://visallo.org/dev#image' },
                    { name: 'http://visallo.org#raw', value: '', metadata: { 'http://visallo.org#mimeType': 'image/png' } } 
                ]
            }
        });
        this.$facebox = this.$node.find('.facebox:not(.editing)')
        this.$faceboxEdit = this.$node.find('.facebox.editing')
    });

    describe('image', function() {

        it('should set image src', function() {
            var img = this.component.select('imageSelector');
            img.attr('src').should.equal(SRC)
        })

        it('should have both facebox\'s hidden on load', function() {
            this.$facebox.is(':visible').should.be.false
            this.$faceboxEdit.is(':visible').should.be.false
        })

        xit('should show facebox on detected object tag hover', function() {
            var box = this.$facebox;

            box.is(':visible').should.be.false

            this.$node.trigger('DetectedObjectEnter', {
                'http://visallo.org#rowKey': ROW_KEY,
                x1: 0, y1: 0,
                x2: 10, y2: 10
            })

            box.is(':visible').should.be.true
            box.css('left').should.equal('0px')
            box.css('top').should.equal('0px')
        })

        it('should hide facebox on detected object leave', function() {
            var box = this.$facebox;

            this.$node.trigger('DetectedObjectLeave', {
                'http://visallo.org#rowKey': ROW_KEY
            })

            box.is(':visible').should.be.false
        })

        xit('should show facebox for resolved entity', function() {

            this.$node.trigger('DetectedObjectEdit', {
                entityVertex: {
                    id: VERTEX_ID,
                    properties: {}
                },
                'http://visallo.org#rowKey': ROW_KEY,
                x1: 0, y1: 0, x2: 10, y2: 10
            })

            this.component.currentlyEditing.should.equal(VERTEX_ID)

            checkFacebox(this.$facebox, { visible: false })
            checkFacebox(this.$faceboxEdit, { visible: true, disabled: true, left: '0px'})

            this.$node.trigger('DetectedObjectEnter', {
                'http://visallo.org#rowKey': ROW_KEY_2,
                x1: EXPECTED_WIDTH / 2,
                y1: 0,
                x2: EXPECTED_WIDTH,
                y2: EXPECTED_WIDTH
            })
            this.$facebox.css('left').should.not.equal('0px')

            checkFacebox(this.$faceboxEdit, { visible: true })

            this.$node.trigger('DetectedObjectLeave', { })
            checkFacebox(this.$faceboxEdit, { visible: true, left: '0px'})

            this.$node.trigger('DetectedObjectLeave', { id: VERTEX_ID })
            checkFacebox(this.$faceboxEdit, { visible: true, left: '0px'})

            this.$node.trigger('DetectedObjectDoneEditing')
            checkFacebox(this.$faceboxEdit, { visible: false })
        })

        xit('should show facebox for unresolved entity', function() {
            this.$node.trigger('DetectedObjectEdit', {
                'http://visallo.org#rowKey': ROW_KEY,
                x1: 0, y1: 0, x2: 10, y2: 10
            })

            this.component.currentlyEditing.should.equal(ROW_KEY)

            checkFacebox(this.$facebox, { visible: false })
            checkFacebox(this.$faceboxEdit, { visible: true, disabled: false, left: '0px'})

            this.$node.trigger('DetectedObjectEnter', {
                'http://visallo.org#rowKey': ROW_KEY_2,
                x1: EXPECTED_WIDTH / 2,
                y1: 0,
                x2: EXPECTED_WIDTH,
                y2: EXPECTED_WIDTH
            })
            checkFacebox(this.$facebox, { visible: true })
            this.$facebox.css('left').should.not.equal('0px')

            checkFacebox(this.$faceboxEdit, { visible: true })

            this.$node.trigger('DetectedObjectLeave', { })
            checkFacebox(this.$faceboxEdit, { visible: true, left: '0px'})

            this.$node.trigger('DetectedObjectDoneEditing')
            checkFacebox(this.$faceboxEdit, { visible: false })
        })

        function checkFacebox(box, options) {
            box.is(':visible').should.equal(options.visible)

            if (!_.isUndefined(options.disabled)) {
                box.hasClass('ui-resizable-disabled').should.equal(options.disabled)
                box.hasClass('ui-draggable-disabled').should.equal(options.disabled)
            }

            if (!_.isUndefined(options.left)) {
                box.css('left').should.equal(options.left)
            }
        }
    })

})
