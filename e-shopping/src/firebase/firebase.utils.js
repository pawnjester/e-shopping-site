import firebase from 'firebase/app';
import 'firebase/auth';
import 'firebase/firestore';

const config = {
  apiKey: "AIzaSyBv-Q87L6EheL-xCvuFW9DP6TLf1aF6WyM",
  authDomain: "crown-db-8ede5.firebaseapp.com",
  databaseURL: "https://crown-db-8ede5.firebaseio.com",
  projectId: "crown-db-8ede5",
  storageBucket: "crown-db-8ede5.appspot.com",
  messagingSenderId: "407624760207",
  appId: "1:407624760207:web:88186825e767024dc3e9fa",
  measurementId: "G-L4BG00DGRT"
};


export const createUserProfileDocument = async (userAuth, additionalData) => {
  if(!userAuth) return;

  const userRef = firestore.doc(`users/${userAuth.uid}`);
  console.log("y>>>>", userAuth);

  const snapshot = await userRef.get();

  if(!snapshot.exists) {
    const {displayName, email } = userAuth;
    const createdAt = new Date();

    try{

      await userRef.set({
        displayName,
        email,
        createdAt,
        ...additionalData
      })
    } catch (error) {
      console.log('cerror creating ', error.message);
    }
  }

  return userRef;

}

firebase.initializeApp(config);
export const firestore = firebase.firestore();

export const auth = firebase.auth();

const provider = new firebase.auth.GoogleAuthProvider();
provider.setCustomParameters({
  prompt: "select_account"
});

export const signInWithGoogle = () => auth.signInWithPopup(provider);


export default firebase;
