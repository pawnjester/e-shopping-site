import React from 'react';
import { Route, Switch } from 'react-router-dom';
import { setCurrentUser } from './redux/user/user.actions';
import { connect } from 'react-redux';
import logo from './logo.svg';
import './App.css';
import Homepage from './pages/homepage/homepage.component';
import ShopPage from './pages/shop/shop.component';
import Header from './components/header/header.component';
import SignInAndSignUpPage from  './pages/sign-in-and-sign-up/sign-in-and-sign-up.component';
import { auth, createUserProfileDocument } from './firebase/firebase.utils.js';



class App extends React.Component {

  unSubscribeFromAuth = null;

  componentDidMount() {

    const { setCurrentUser } = this.props;

    this.unSubscribeFromAuth = auth.onAuthStateChanged(async userAuth => {
      if(userAuth) {
        const userRef = await createUserProfileDocument(userAuth);

        userRef.onSnapshot(snapshot => {
          setCurrentUser ({
              id: snapshot.id,
              ...snapshot.data()
            });
          });
      }
      setCurrentUser(userAuth )
    })
  }

  componentWillUnmount() {
    this.unSubscribeFromAuth();  //we have unsubscribe to manage leaks
  }

  render() {
    return (
      <div>
        <Header />
        <Switch>
          <Route exact path="/" component= {Homepage} />
          <Route path="/shop" component= {ShopPage} />
          <Route path="/login" component= {SignInAndSignUpPage} />
        </Switch>
      </div>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  setCurrentUser: user => dispatch(setCurrentUser(user))
})

export default connect(null, mapDispatchToProps)(App);
