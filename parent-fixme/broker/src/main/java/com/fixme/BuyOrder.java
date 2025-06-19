// Send a message through the Router to the Market to attempt to buy a certain product
// Broker must await an answer before trying another order
// Answer can be : positive -> order executed and market stock changed
// or negative -> order refused (not enough stock, non existing good, internal error...)