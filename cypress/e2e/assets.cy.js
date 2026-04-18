describe('Asset Files & Data Integrity', () => {

  describe('Required Files', () => {
    const files = [
      'index.html', 'dashboard.html', 'hondata.html', 'streetview.html',
      'multisystem.html', 'carview.html', 'music.html', 'battery.html',
      'config.js', 'demo-routes.js', 'flashpro-reference.json', 'flashpro-docs.txt'
    ]

    files.forEach(file => {
      it(`${file} loads successfully`, () => {
        cy.request('/' + file).its('status').should('eq', 200)
      })
    })
  })

  describe('FlashPro Reference JSON', () => {
    it('is valid JSON', () => {
      cy.request('/flashpro-reference.json').then(resp => {
        const data = typeof resp.body === 'string' ? JSON.parse(resp.body) : resp.body
        expect(data).to.be.an('object')
        expect(data).to.have.property('_meta')
      })
    })

    it('has all required sections', () => {
      cy.request('/flashpro-reference.json').then(resp => {
        const data = typeof resp.body === 'string' ? JSON.parse(resp.body) : resp.body
        expect(data).to.have.property('sensors')
        expect(data).to.have.property('calibration_tables')
        expect(data).to.have.property('parameters')
        expect(data).to.have.property('vehicle_notes')
        expect(data).to.have.property('bluetooth')
        expect(data).to.have.property('traction_control')
      })
    })

    it('has K20Z3-relevant sensors', () => {
      cy.request('/flashpro-reference.json').then(resp => {
        const data = typeof resp.body === 'string' ? JSON.parse(resp.body) : resp.body
        expect(data.sensors).to.have.property('RPM')
        expect(data.sensors).to.have.property('MAP')
        expect(data.sensors).to.have.property('VSS')
        expect(data.sensors).to.have.property('ECT')
        expect(data.sensors).to.have.property('IAT')
        expect(data.sensors).to.have.property('IGN')
      })
    })

    it('sensor ranges are valid', () => {
      cy.request('/flashpro-reference.json').then(resp => {
        const data = typeof resp.body === 'string' ? JSON.parse(resp.body) : resp.body
        const rpm = data.sensors.RPM
        expect(rpm.range[0]).to.eq(0)
        expect(rpm.range[1]).to.eq(9000)
        expect(rpm.unit).to.eq('rpm')
      })
    })
  })

  describe('Demo Routes', () => {
    it('has valid route coordinates', () => {
      cy.request('/demo-routes.js').then(resp => {
        // Check that the file contains coordinate arrays
        expect(resp.body).to.include('EPIC_LOOP_PATH')
        expect(resp.body).to.include('LOCAL_LOOP_PATH')
        // Check coordinates are in Massachusetts range
        expect(resp.body).to.include('-71.')
        expect(resp.body).to.include('42.')
      })
    })
  })

  describe('Config', () => {
    it('has Mapbox token', () => {
      cy.request('/config.js').then(resp => {
        expect(resp.body).to.include('MAPBOX_TOKEN')
        expect(resp.body).to.include('pk.ey')
      })
    })

    it('has Spotify client ID', () => {
      cy.request('/config.js').then(resp => {
        expect(resp.body).to.include('SPOTIFY_CLIENT_ID')
      })
    })
  })

  describe('K20Z3 Accuracy', () => {
    it('hondata.html has correct K20Z3 data', () => {
      cy.request('/hondata.html').then(resp => {
        // Stock values
        expect(resp.body).to.include('310')      // 310cc injectors
        expect(resp.body).to.include('5800')     // VTEC RPM
        expect(resp.body).to.include('kPa')      // MAP units
        expect(resp.body).to.include('K20Z3')    // Engine code
        expect(resp.body).to.include('FA5')      // Chassis code

        // 6 calibration tables
        expect(resp.body).to.include('fuel_hi')
        expect(resp.body).to.include('fuel_lo')
        expect(resp.body).to.include('ign_hi')
        expect(resp.body).to.include('ign_lo')
        expect(resp.body).to.include('cam_hi')
        expect(resp.body).to.include('cam_lo')

        // Honda-specific DTCs
        expect(resp.body).to.include('P2646')
        expect(resp.body).to.include('P0300')
      })
    })

    it('hondata.html is read-only (no write operations)', () => {
      cy.request('/hondata.html').then(resp => {
        // Should not have active edit functionality
        expect(resp.body).to.not.include('ondblclick="editCell')
        // Should not have FLASH tab
        expect(resp.body).to.not.include("switchTab('flash')")
      })
    })
  })
})
