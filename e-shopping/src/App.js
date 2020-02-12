import React from 'react';
import { Route } from 'react-router-dom';
import logo from './logo.svg';
import './App.css';
import Homepage from './pages/homepage/homepage.component';


const TopicsPage =() => (
  <div>
    <h1>
      Topic Detail
    </h1>
  </div>
)
function App() {
  return (
    <div>
        <Route exact path="/" component= {Homepage} />
        <Route exact path="/shop/hats" component= {TopicsPage} />
    </div>
  );
}

export default App;
