import React from 'react';
import { Route, Switch } from 'react-router-dom';
import logo from './logo.svg';
import './App.css';
import Homepage from './pages/homepage/homepage.component';
import ShopPage from './pages/shop/shop.component';
import Header from './components/header/header.component';
import SignInAndSignUpPage from  './pages/sign-in-and-sign-up/sign-in-and-sign-up.component';
import { auth } from './firebase/firebase.utils.js';



class App extends React.Component {

  constructor(props) {
    super(props)

    this.state = {
      currentUser: null
    }
  }

  unSubscribeFromAuth = null;

  componentDidMount() {
    this.unSubscribeFromAuth = auth.onAuthStateChanged(user => {
      this.setState ({
        currentUser: user
      })

      console.log(user);
    })
  }

  componentWillUnmount() {
    this.unSubscribeFromAuth();  //we have unsubscribe to manage leaks
  }

  render() {
    return (
      <div>
        <Header currentUser={this.state.currentUser}/>
        <Switch>
          <Route exact path="/" component= {Homepage} />
          <Route path="/shop" component= {ShopPage} />
          <Route path="/login" component= {SignInAndSignUpPage} />
        </Switch>
      </div>
    );
  }
}

export default App;
