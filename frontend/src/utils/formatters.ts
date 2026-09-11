const usdFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
})

const numberFormatter = new Intl.NumberFormat('en-US')

export function formatMoney(value: number): string {
  return usdFormatter.format(value)
}

export function formatNumber(value: number): string {
  return numberFormatter.format(value)
}

export function formatMileage(value: number): string {
  return `${numberFormatter.format(value)} km`
}

export function formatConsumption(value: number): string {
  return `${value} L/100km`
}