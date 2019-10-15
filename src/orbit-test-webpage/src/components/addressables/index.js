import React from 'react'
import './style.css'

import { Table } from 'antd'

const tableColumns = [
  {
    title: 'Type',
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: 'Id',
    dataIndex: 'id',
    key: 'id'
  }  
]

export default function Addressables(props) {
  const { addressables, select } = props;
  return (<section className="addressables">
    <h3>Addressables</h3>
      <ol>
        {addressables.map(a => <li onClick={() => select(a)}>{a}</li>)}
      </ol>
  </section>
  )
}
