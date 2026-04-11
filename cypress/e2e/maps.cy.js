describe('Maps Page', () => {
  beforeEach(() => {
    cy.visit('/streetview.html')
  })

  describe('Navigation View', () => {
    it('shows nav bar with distance and street', () => {
      cy.get('.nav-bar').should('exist')
      cy.get('#nav-distance').should('exist')
      cy.get('#nav-street').should('exist')
    })

    it('shows speed pill', () => {
      cy.get('.speed-pill').should('exist')
      cy.get('#speed-val').should('exist')
    })

    it('shows drive/reset/brake controls', () => {
      cy.get('#btn-drive').should('contain', 'DRIVE')
      cy.contains('RESET').should('exist')
      cy.contains('BRAKE').should('exist')
    })

    it('shows ETA', () => {
      cy.get('#nav-eta').should('exist')
    })

    it('has map container', () => {
      cy.get('#map').should('exist')
    })

    it('has recenter button', () => {
      cy.get('.recenter-btn').should('exist')
    })
  })

  describe('Trip Logbook', () => {
    it('has trip toggle button', () => {
      cy.get('.trip-toggle').should('exist')
    })

    it('trip panel is hidden initially', () => {
      cy.get('.trip-panel').should('not.have.class', 'open')
    })

    it('clicking toggle opens trip panel', () => {
      cy.get('.trip-toggle').click()
      cy.get('.trip-panel').should('have.class', 'open')
    })

    it('trip panel has back button', () => {
      cy.get('.trip-toggle').click()
      cy.get('.tp-back').should('exist')
    })

    it('trip panel has title "Trips"', () => {
      cy.get('.trip-toggle').click()
      cy.get('.tp-title').should('contain', 'Trips')
    })

    it('trip panel shows status badge', () => {
      cy.get('.trip-toggle').click()
      cy.get('#tp-status').should('contain', 'STANDBY')
    })

    it('trip panel has export and clear buttons', () => {
      cy.get('.trip-toggle').click()
      cy.contains('Export CSV').should('exist')
      cy.contains('Clear All').should('exist')
    })

    it('back button closes trip panel', () => {
      cy.get('.trip-toggle').click()
      cy.get('.trip-panel').should('have.class', 'open')
      cy.get('.tp-back').click()
      cy.get('.trip-panel').should('not.have.class', 'open')
    })
  })

  describe('Demo Trips', () => {
    beforeEach(() => {
      // Clear and reload to get fresh demo data
      cy.window().then(win => {
        win.localStorage.removeItem('hondadash_trips_v2')
      })
      cy.reload()
      cy.get('.trip-toggle').click()
      cy.wait(500) // wait for demo data to seed
    })

    it('loads demo trips', () => {
      cy.get('.tp-card').should('have.length.greaterThan', 0)
    })

    it('trip cards show distance', () => {
      cy.get('.tp-card-dist').first().should('not.be.empty')
    })

    it('trip cards show date', () => {
      cy.get('.tp-card-date').first().should('not.be.empty')
    })

    it('trip cards show stats', () => {
      cy.get('.tp-card-stats').first().should('not.be.empty')
    })

    it('trip cards have map thumbnails', () => {
      cy.get('.tp-card-map').should('have.length.greaterThan', 0)
    })

    it('clicking trip card opens detail view', () => {
      cy.get('.tp-card').first().click()
      cy.get('.trip-detail').should('have.class', 'open')
    })

    it('detail view has back button', () => {
      cy.get('.tp-card').first().click()
      cy.get('.td-back').should('exist')
    })

    it('detail view shows distance', () => {
      cy.get('.tp-card').first().click()
      cy.get('.td-dist').should('not.be.empty')
    })

    it('detail view shows performance stats', () => {
      cy.get('.tp-card').first().click()
      cy.contains('MAX RPM').should('exist')
      cy.contains('VTEC').should('exist')
    })

    it('detail view shows fuel economy', () => {
      cy.get('.tp-card').first().click()
      cy.contains('MPG').should('exist')
    })

    it('detail view shows datalog graphs', () => {
      cy.get('.tp-card').first().click()
      cy.contains('DATALOG').should('exist')
      cy.get('#dl-speed').should('exist')
      cy.get('#dl-rpm').should('exist')
    })

    it('detail view back button closes detail', () => {
      cy.get('.tp-card').first().click()
      cy.get('.trip-detail').should('have.class', 'open')
      cy.get('.td-back').click()
      cy.get('.trip-detail').should('not.have.class', 'open')
    })
  })

  describe('Trip Data Bridge', () => {
    it('updateTripData function exists', () => {
      cy.window().then(win => {
        expect(win.updateTripData).to.be.a('function')
      })
    })

    it('accepts object format with full sensor data', () => {
      cy.window().then(win => {
        // Should not throw
        win.updateTripData({ spd: 45, rpm: 3500, ect: 190, lat: 42.37, lng: -71.24, map: 85, afr: 14.7, ign: 32 })
      })
    })

    it('accepts legacy format (spd, rpm, clt, lat, lng)', () => {
      cy.window().then(win => {
        win.updateTripData(45, 3500, 190, 42.37, -71.24)
      })
    })
  })
})
