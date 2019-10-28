import { createStore } from "redux";

import reducer from "./reducer";

export default function configureStore(initialState) {
    return createStore(reducer, initialState);
}