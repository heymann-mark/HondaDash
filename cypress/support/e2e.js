// Custom commands for HondaDash testing

// Switch to a page tab in index.html
Cypress.Commands.add('switchTab', (tabName) => {
  cy.get('.nav-btn').contains(tabName, { matchCase: false }).click()
})

// Wait for an iframe to load and get its body
Cypress.Commands.add('getIframeBody', (iframeSelector) => {
  return cy.get(iframeSelector)
    .its('0.contentDocument.body')
    .should('not.be.empty')
    .then(cy.wrap)
})

// Suppress uncaught exceptions from mapbox/third-party libs
Cypress.on('uncaught:exception', (err) => {
  if (err.message.includes('mapboxgl') || err.message.includes('turf') || err.message.includes('Mapbox')) {
    return false
  }
})
