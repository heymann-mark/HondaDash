describe('App Navigation', () => {
  beforeEach(() => {
    cy.visit('/index.html')
  })

  it('loads the app with 6 nav tabs', () => {
    cy.get('.nav-btn').should('have.length', 6)
  })

  it('shows correct tab labels', () => {
    cy.get('.nav-btn').eq(0).should('contain', 'GAUGES')
    cy.get('.nav-btn').eq(1).should('contain', 'VEHICLE')
    cy.get('.nav-btn').eq(2).should('contain', 'MAPS')
    cy.get('.nav-btn').eq(3).should('contain', 'MUSIC')
    cy.get('.nav-btn').eq(4).should('contain', 'BATTERY')
    cy.get('.nav-btn').eq(5).should('contain', 'FLASHPRO')
  })

  it('GAUGES tab is active by default', () => {
    cy.get('.nav-btn').eq(0).should('have.class', 'active')
    cy.get('#frame-gauges').should('have.class', 'active')
  })

  it('switches between tabs', () => {
    cy.get('.nav-btn').contains('VEHICLE').click()
    cy.get('.nav-btn').contains('VEHICLE').should('have.class', 'active')
    cy.get('#frame-vehicle').should('have.class', 'active')
    cy.get('#frame-gauges').should('not.have.class', 'active')
  })

  it('only one tab is active at a time', () => {
    const tabs = ['VEHICLE', 'MAPS', 'MUSIC', 'BATTERY', 'FLASHPRO']
    tabs.forEach(tab => {
      cy.get('.nav-btn').contains(tab).click()
      cy.get('.nav-btn.active').should('have.length', 1)
      cy.get('.page-frame.active').should('have.length', 1)
    })
  })

  it('does not have removed tabs (MULTI, DATALOG, HONDATA, TUNER, TRIPS)', () => {
    cy.get('.nav-btn').should('not.contain', 'MULTI')
    cy.get('.nav-btn').should('not.contain', 'DATALOG')
    cy.get('.nav-btn').should('not.contain', 'HONDATA')
    cy.get('.nav-btn').should('not.contain', 'TUNER')
    cy.get('.nav-btn').should('not.contain', 'TRIPS')
  })
})
