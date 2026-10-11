import { Route, Routes } from 'react-router-dom'

import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import Account from './pages/Account'
import AdminPage from './pages/admin/AdminPage'
import CartPage from './pages/CartPage'
import Checkout from './pages/Checkout'
import ForgotPassword from './pages/ForgotPassword'
import Home from './pages/Home'
import Login from './pages/Login'
import OrderPage from './pages/OrderPage'
import Orders from './pages/Orders'
import ProductPage from './pages/ProductPage'
import Register from './pages/Register'
import Search from './pages/Search'
import SellerPage from './pages/seller/SellerPage'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="search" element={<Search />} />
        <Route path="products/:id" element={<ProductPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="forgot-password" element={<ForgotPassword />} />
        <Route path="checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
        <Route path="orders" element={<RequireAuth><Orders /></RequireAuth>} />
        <Route path="orders/:number" element={<RequireAuth><OrderPage /></RequireAuth>} />
        <Route path="account" element={<RequireAuth><Account /></RequireAuth>} />
        <Route path="seller" element={<RequireAuth><SellerPage /></RequireAuth>} />
        <Route path="admin" element={<RequireAuth role="ADMIN"><AdminPage /></RequireAuth>} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}

function NotFound() {
  return (
    <div className="py-20 text-center">
      <p className="text-5xl font-extrabold text-ink">404</p>
    </div>
  )
}
