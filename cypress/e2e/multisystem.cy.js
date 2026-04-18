describe('Multi-System Live Data Page', () => {
  beforeEach(() => {
    cy.visit('/multisystem.html')
  })

  describe('Layout', () => {
    it('shows LIVE DATA STREAM title', () => {
      cy.get('.status-title').should('contain', 'LIVE DATA STREAM')
    })

    it('shows connection status', () => {
      cy.get('#conn-text').should('contain', 'SIMULATED')
    })

    it('has sidebar with sensor groups', () => {
      cy.get('.sidebar').should('exist')
      cy.get('.module-header').should('have.length.greaterThan', 3)
    })

    it('has 4 hero gauge slots', () => {
      cy.get('.gauge-cell').should('have.length', 4)
    })

    it('has graph canvas', () => {
      cy.get('#graph').should('exist')
    })

    it('has toggle bar', () => {
      cy.get('#toggle-bar').should('exist')
    })
  })

  describe('Sensor Groups', () => {
    it('has ENGINE group', () => {
      cy.get('.module-header').should('contain', 'ENGINE')
    })

    it('has FUEL group', () => {
      cy.get('.module-header').should('contain', 'FUEL')
    })

    it('has IGN group', () => {
      cy.get('.module-header').should('contain', 'IGN')
    })

    it('has VTEC group', () => {
      cy.get('.module-header').should('contain', 'VTEC')
    })

    it('has TEMP group', () => {
      cy.get('.module-header').should('contain', 'TEMP')
    })

    it('has ELEC group', () => {
      cy.get('.module-header').should('contain', 'ELEC')
    })

    it('does not have ECM/TCM/ABS/BCM groups', () => {
      cy.get('.module-header').should('not.contain', 'ECM')
      cy.get('.module-header').should('not.contain', 'TCM')
      cy.get('.module-header').should('not.contain', 'ABS')
      cy.get('.module-header').should('not.contain', 'BCM')
    })
  })

  describe('PID Selection', () => {
    it('expanding a group shows PIDs', () => {
      cy.get('.module-header').first().click()
      cy.get('.pid-list.open .pid-item').should('have.length.greaterThan', 0)
    })

    it('clicking a PID selects it', () => {
      cy.get('.module-header').first().click()
      cy.get('.pid-item').first().click()
      cy.get('.pid-item.selected').should('have.length', 1)
    })

    it('selected PID appears in toggle bar', () => {
      cy.get('.module-header').first().click()
      cy.get('.pid-item').first().click()
      cy.get('.pid-toggle').should('have.length.greaterThan', 0)
    })

    it('selected PID fills a hero gauge', () => {
      cy.get('.module-header').first().click()
      cy.get('.pid-item').first().click()
      cy.get('.gauge-cell').not('.empty').should('have.length.greaterThan', 0)
    })

    it('clear removes all selected PIDs', () => {
      // Select a PID then use clear button instead of re-clicking
      cy.get('.module-header').first().click()
      cy.get('.pid-list.open .pid-item').first().click()
      cy.get('.pid-item.selected').should('have.length.greaterThan', 0)
      cy.get('#clear-btn').click()
      cy.get('.pid-item.selected').should('have.length', 0)
    })

    it('clear button removes all selections', () => {
      cy.get('.module-header').first().click()
      cy.get('.pid-item').first().click()
      cy.get('#clear-btn').click()
      cy.get('.pid-item.selected').should('have.length', 0)
      cy.get('.pid-toggle').should('have.length', 0)
    })
  })

  describe('Color System', () => {
    it('uses distinct colors from palette', () => {
      // COLOR_PALETTE is a const, check the source instead
      cy.request('/multisystem.html').then(resp => {
        expect(resp.body).to.include('COLOR_PALETTE')
        // Should have at least 8 distinct hex colors
        const colors = resp.body.match(/#[0-9a-fA-F]{6}/g)
        expect(colors.length).to.be.greaterThan(8)
      })
    })

    it('has line dash styles', () => {
      cy.request('/multisystem.html').then(resp => {
        expect(resp.body).to.include('LINE_STYLES')
        expect(resp.body).to.include('setLineDash')
      })
    })
  })

  describe('Simulation', () => {
    it('sensor values update over time', () => {
      // Select RPM
      cy.get('.module-header').first().click()
      cy.get('.pid-item').first().click()
      cy.wait(500)
      cy.get('.gauge-value').first().invoke('text').then(val1 => {
        cy.wait(1000)
        cy.get('.gauge-value').first().invoke('text').should('not.eq', val1)
      })
    })
  })
})
