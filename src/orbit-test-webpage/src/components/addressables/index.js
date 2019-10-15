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

  const rows = addressables.map(a => ({
    ...a,
    key: `${a.type}-${a.id}`
  }))

  return (<section className="addressables">
    <h3>Addressables</h3>
    <Table dataSource={rows} columns={tableColumns} onRow={(record) => {
      return {
        onClick: () => select(record.key)
      }
    }}
    />
  </section>
  )
}
