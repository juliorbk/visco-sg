package com.visco.backend.models.entities;

public enum Currency {
  // 🇻🇪 Venezuela
  VED, // Bolívar Digital (Uso local/referencial)
  VES, // Bolívar Soberano (Código ISO 4217 Oficial actual)

  // 🌎 Resto de América Latina y Caribe
  ARS, // Peso Argentino
  BOB, // Boliviano
  BRL, // Real Brasileño
  CLP, // Peso Chileno
  COP, // Peso Colombiano
  CRC, // Colón Costarricense
  CUP, // Peso Cubano
  DOP, // Peso Dominicano
  GTQ, // Quetzal Guatemalteco
  HNL, // Lempira Hondureño
  MXN, // Peso Mexicano
  NIO, // Córdoba Nicaragüense
  PAB, // Balboa Panameño (Equivalente al USD)
  PEN, // Sol Peruano
  PYG, // Guaraní Paraguayo
  UYU, // Peso Uruguayo

  // 🇺🇸 Norteamérica
  USD, // Dólar Estadounidense
  CAD, // Dólar Canadiense

  // 🇪🇺 Europa
  EUR, // Euro
  GBP, // Libra Esterlina (Reino Unido)
  CHF, // Franco Suizo
  SEK, // Corona Sueca
  NOK, // Corona Noruega
  DKK, // Corona Danesa
  RUB, // Rublo Ruso

  // 🌏 Asia y Oceanía
  JPY, // Yen Japonés
  CNY, // Yuan Chino (RMB)
  INR, // Rupia India
  AUD, // Dólar Australiano
  NZD, // Dólar Neozelandés
  KRW, // Won Surcoreano
  SGD, // Dólar Singapurense
  HKD, // Dólar de Hong Kong

  // 🌍 Medio Oriente y África
  AED, // Dírham de los Emiratos Árabes Unidos
  SAR, // Riyal Saudí
  ZAR, // Rand Sudafricano
  EGP, // Libra Egipcia
}
