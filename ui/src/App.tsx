import React from 'react'
import {
  Masthead,
  MastheadLogo,
  MastheadMain,
  Page,
  PageSection,
} from '@patternfly/react-core'
import HeatpumpUpload from './components/HeatpumpUpload'

const masthead = (
  <Masthead>
    <MastheadMain>
      <MastheadLogo>🔥 Heatpump Manager</MastheadLogo>
    </MastheadMain>
  </Masthead>
)

const App: React.FC = () => (
  <Page masthead={masthead}>
    <PageSection>
      <HeatpumpUpload />
    </PageSection>
  </Page>
)

export default App
