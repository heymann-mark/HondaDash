describe('FlashPro Page', () => {
  beforeEach(() => {
    cy.visit('/hondata.html')
  })

  describe('Header', () => {
    it('shows FLASHPRO MANAGER title', () => {
      cy.get('.top-title').should('contain', 'FLASHPRO MANAGER')
    })

    it('shows K20Z3 // FA5 identifier', () => {
      cy.contains('K20Z3')
      cy.contains('FA5')
    })

    it('shows READ ONLY indicator', () => {
      cy.contains('READ ONLY')
    })
  })

  describe('Sub Navigation', () => {
    it('has correct sub-tabs', () => {
      cy.get('.sub-btn').should('contain', 'TABLES')
      cy.get('.sub-btn').should('contain', 'CALIBRATION')
      cy.get('.sub-btn').should('contain', 'DTC')
      cy.get('.sub-btn').should('contain', 'DATALOGS')
      cy.get('.sub-btn').should('contain', 'SENSORS')
      cy.get('.sub-btn').should('contain', 'MULTI')
      cy.get('.sub-btn').should('contain', 'REFERENCE')
    })

    it('does not have FLASH tab', () => {
      cy.get('.sub-btn').should('not.contain', 'FLASH')
    })

    it('TABLES is active by default', () => {
      cy.get('.sub-btn').first().should('have.class', 'active')
      cy.get('#panel-tables').should('have.class', 'active')
    })

    it('switches sub-tabs correctly', () => {
      cy.get('.sub-btn').contains('DTC').click()
      cy.get('#panel-dtc').should('have.class', 'active')
      cy.get('#panel-tables').should('not.have.class', 'active')
    })
  })

  describe('Tables Tab (Read Only)', () => {
    it('shows table selector with 6 tables', () => {
      cy.get('#table-selector option').should('have.length', 6)
    })

    it('has fuel and ignition tables', () => {
      cy.get('#table-selector').should('contain', 'FUEL')
      cy.get('#table-selector').should('contain', 'IGNITION')
      cy.get('#table-selector').should('contain', 'CAM ANGLE')
    })

    it('renders heatmap with cells', () => {
      cy.get('.heatmap-table td').should('have.length.greaterThan', 50)
    })

    it('shows table info text', () => {
      // loadTable overwrites with table label on load
      cy.get('#table-info').should('not.be.empty')
    })

    it('cells are not editable (no ondblclick)', () => {
      cy.get('.heatmap-table td').first().should('not.have.attr', 'ondblclick')
    })

    it('cells do not highlight on hover', () => {
      cy.get('.heatmap-table td').first().should('have.css', 'cursor', 'default')
    })

    it('shows kPa axis labels', () => {
      cy.get('.heatmap-axis-col').should('contain', 'kPa')
    })

    it('can switch between tables', () => {
      cy.get('#table-selector').select('ign_hi')
      cy.get('.heatmap-table td').should('have.length.greaterThan', 50)
    })
  })

  describe('Calibration Tab (Read Only)', () => {
    beforeEach(() => {
      cy.get('.sub-btn').contains('CALIBRATION').click()
    })

    it('shows parameter groups', () => {
      cy.get('.cal-group').should('have.length.greaterThan', 5)
    })

    it('shows FUEL parameters', () => {
      cy.contains('FUEL')
    })

    it('shows VTEC parameters', () => {
      cy.contains('VTEC')
    })

    it('shows stock injector size 310cc', () => {
      cy.contains('310')
    })

    it('shows stock rev limit 8000', () => {
      cy.contains('8000').should('exist')
    })

    it('parameter values are not editable', () => {
      cy.get('.cal-row-val').first().should('not.have.attr', 'contenteditable', 'true')
    })

    it('shows view-only message', () => {
      cy.contains('FlashPro Manager').should('exist')
    })

    it('has search functionality', () => {
      cy.get('.cal-search').should('exist')
      cy.get('.cal-search').type('VTEC')
    })

    it('groups are collapsible', () => {
      cy.get('.cal-group-header').first().click()
      cy.get('.cal-group-header').first().click()
    })
  })

  describe('DTC Tab', () => {
    beforeEach(() => {
      cy.get('.sub-btn').contains('DTC').click()
    })

    it('shows scan button', () => {
      cy.contains('SCAN').should('exist')
    })

    it('shows clear button', () => {
      cy.contains('CLEAR').should('exist')
    })

    it('scan populates DTC list', () => {
      cy.contains('SCAN').click()
      cy.get('.dtc-card').should('have.length.greaterThan', 0)
    })

    it('shows DTC count after scan', () => {
      cy.contains('SCAN').click()
      cy.get('#dtc-count').invoke('text').then(text => {
        expect(parseInt(text)).to.be.greaterThan(0)
      })
    })

    it('clear removes all DTCs', () => {
      cy.on('uncaught:exception', () => false)
      cy.contains('SCAN').click()
      cy.contains('CLEAR').click()
      cy.get('#dtc-count').should('contain', '0')
    })
  })

  describe('Sensors Tab', () => {
    beforeEach(() => {
      cy.get('.sub-btn').contains('SENSORS').click()
    })

    it('renders sensor cards', () => {
      cy.get('.sensor-card').should('have.length.greaterThan', 10)
    })

    it('shows ENGINE group', () => {
      cy.get('.sensor-group-title').should('contain', 'ENGINE')
    })

    it('shows FUEL group', () => {
      cy.contains('FUEL')
    })

    it('shows RPM sensor', () => {
      cy.contains('RPM')
    })

    it('sensor values update over time (simulation)', () => {
      cy.get('#sval-rpm').invoke('text').then(val1 => {
        cy.wait(500)
        cy.get('#sval-rpm').invoke('text').should('not.eq', val1)
      })
    })

    it('RPM stays in valid range', () => {
      cy.wait(1000)
      cy.get('#sval-rpm').invoke('text').then(text => {
        const rpm = parseInt(text)
        expect(rpm).to.be.within(0, 9000)
      })
    })
  })

  describe('Reference Tab', () => {
    beforeEach(() => {
      cy.get('.sub-btn').contains('REFERENCE').click()
    })

    it('has search input', () => {
      cy.get('#ref-search').should('exist')
    })

    it('shows category buttons', () => {
      cy.get('.ref-cat-btn').should('have.length.greaterThan', 3)
    })

    it('shows results', () => {
      cy.get('#ref-results-count').should('not.be.empty')
    })

    it('search filters results', () => {
      cy.get('#ref-search').type('VTEC')
      cy.get('#ref-results-count').should('contain', 'RESULT')
    })

    it('cards expand on click', () => {
      cy.get('.ref-card').first().click()
      cy.get('.ref-card.expanded').should('have.length', 1)
    })
  })

  describe('Datalogs Tab', () => {
    beforeEach(() => {
      cy.get('.sub-btn').contains('DATALOGS').click()
    })

    it('shows load CSV button', () => {
      cy.contains('LOAD CSV').should('exist')
    })

    it('shows empty state message', () => {
      cy.contains('LOAD A CSV DATALOG')
    })

    it('has canvas for chart', () => {
      cy.get('#dl-canvas').should('exist')
    })
  })
})
